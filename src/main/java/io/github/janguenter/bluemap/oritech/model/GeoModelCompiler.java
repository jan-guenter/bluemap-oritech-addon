/*
 * SPDX-License-Identifier: MIT
 *
 * Independently authored static interpreter for the installed Bedrock GEO
 * schema. Transform and UV behavior follows GeckoLib's documented/runtime
 * format contract; third-party resources remain outside this add-on.
 */
package io.github.janguenter.bluemap.oritech.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Quad;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Vec3;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Vertex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compiles one exact Oritech Bedrock GEO into immutable default-pose quads. */
public final class GeoModelCompiler {

    private static final Vec3 ZERO = new Vec3(0D, 0D, 0D);
    private static final int MAX_BONE_DEPTH = 64;

    private GeoModelCompiler() {
    }

    public static OritechGeoModel compile(String name, byte[] raw) {
        JsonObject root = object(JsonParser.parseString(
                new String(raw, StandardCharsets.UTF_8)
        ), "root");
        if (!"1.12.0".equals(string(root.get("format_version")))) {
            throw new IllegalArgumentException("unsupported Oritech GEO version");
        }
        JsonArray geometries = array(root.get("minecraft:geometry"), "geometry");
        if (geometries.size() != 1) {
            throw new IllegalArgumentException("Oritech GEO geometry count changed");
        }
        JsonObject geometry = object(geometries.get(0), "geometry");
        JsonObject description = object(geometry.get("description"), "description");
        int textureWidth = positiveInt(description.get("texture_width"));
        int textureHeight = positiveInt(description.get("texture_height"));
        Map<String, RawBone> bones = parseBones(array(geometry.get("bones"), "bones"));

        List<Quad> quads = new ArrayList<>();
        for (RawBone bone : bones.values()) {
            List<RawBone> chain = boneChain(bone, bones);
            for (RawCube cube : bone.cubes) {
                emitCube(cube, bone, chain, textureWidth, textureHeight, quads);
            }
        }
        return new OritechGeoModel(name, quads);
    }

    private static Map<String, RawBone> parseBones(JsonArray source) {
        Map<String, RawBone> bones = new LinkedHashMap<>();
        for (JsonElement element : source) {
            JsonObject object = object(element, "bone");
            String name = string(object.get("name"));
            String parent = object.has("parent") ? string(object.get("parent")) : null;
            Vec3 pivot = object.has("pivot") ? signedPivot(object.get("pivot")) : ZERO;
            Vec3 rotation = object.has("rotation") ? signedRotation(object.get("rotation")) : ZERO;
            Boolean mirror = object.has("mirror") ? bool(object.get("mirror")) : null;
            Double inflate = object.has("inflate") ? number(object.get("inflate")) : null;
            List<RawCube> cubes = new ArrayList<>();
            if (object.has("cubes")) {
                for (JsonElement cube : array(object.get("cubes"), "cubes")) {
                    cubes.add(parseCube(object(cube, "cube")));
                }
            }
            if (bones.put(name, new RawBone(
                    name, parent, pivot, rotation, mirror, inflate, List.copyOf(cubes)
            )) != null) {
                throw new IllegalArgumentException("duplicate Oritech GEO bone");
            }
        }
        return bones;
    }

    private static RawCube parseCube(JsonObject object) {
        Vec3 rawOrigin = vector(object.get("origin"), "cube origin");
        Vec3 size = vector(object.get("size"), "cube size");
        if (size.x() < 0D || size.y() < 0D || size.z() < 0D) {
            throw new IllegalArgumentException("negative Oritech GEO cube size");
        }
        Vec3 origin = new Vec3(
                -(rawOrigin.x() + size.x()) / 16D,
                rawOrigin.y() / 16D,
                rawOrigin.z() / 16D
        );
        Vec3 rotation = object.has("rotation") ? signedRotation(object.get("rotation")) : ZERO;
        Vec3 pivot = object.has("pivot") ? signedPivot(object.get("pivot")) : ZERO;
        Double inflate = object.has("inflate") ? number(object.get("inflate")) : null;
        Boolean mirror = object.has("mirror") ? bool(object.get("mirror")) : null;
        JsonElement uv = object.get("uv");
        if (uv == null || !(uv.isJsonArray() || uv.isJsonObject())) {
            throw new IllegalArgumentException("missing Oritech GEO UV");
        }
        return new RawCube(origin, size, rotation, pivot, inflate, mirror, uv.deepCopy());
    }

    private static List<RawBone> boneChain(RawBone bone, Map<String, RawBone> bones) {
        List<RawBone> chain = new ArrayList<>();
        RawBone current = bone;
        while (current != null) {
            if (chain.size() >= MAX_BONE_DEPTH || chain.contains(current)) {
                throw new IllegalArgumentException("cyclic or deep Oritech GEO hierarchy");
            }
            chain.add(current);
            if (current.parent == null) {
                current = null;
            } else {
                current = bones.get(current.parent);
                if (current == null) {
                    throw new IllegalArgumentException("missing Oritech GEO parent bone");
                }
            }
        }
        return chain;
    }

    private static void emitCube(
            RawCube cube,
            RawBone bone,
            List<RawBone> chain,
            int textureWidth,
            int textureHeight,
            List<Quad> output
    ) {
        double inflation = (cube.inflate != null
                ? cube.inflate : bone.inflate != null ? bone.inflate : 0D) / 16D;
        boolean mirror = cube.mirror != null ? cube.mirror : Boolean.TRUE.equals(bone.mirror);
        Vec3 vertexSize = cube.size.scale(1D / 16D);
        VertexSet vertices = new VertexSet(cube.origin, vertexSize, inflation);
        boolean boxUv = cube.uv.isJsonArray();
        for (Face face : Face.values()) {
            if (zeroSizeFace(cube.size, face)) {
                continue;
            }
            UvRect uv = boxUv
                    ? boxUv(cube.uv.getAsJsonArray(), cube.size, face)
                    : mappedUv(cube.uv.getAsJsonObject(), face);
            if (uv == null) {
                continue;
            }
            Vec3[] faceVertices = vertices.forFace(face, boxUv, mirror);
            double u = uv.u / textureWidth;
            double v = uv.v / textureHeight;
            double uWidth = (uv.u + uv.width) / textureWidth;
            double vHeight = (uv.v + uv.height) / textureHeight;
            if (!mirror) {
                double temporary = uWidth;
                uWidth = u;
                u = temporary;
            }
            float[][] coordinates = {
                    {(float) u, (float) v},
                    {(float) uWidth, (float) v},
                    {(float) uWidth, (float) vHeight},
                    {(float) u, (float) vHeight}
            };
            Vertex[] transformed = new Vertex[4];
            for (int index = 0; index < transformed.length; index++) {
                Vec3 point = faceVertices[index].rotateAbout(cube.pivot, cube.rotation);
                for (RawBone transform : chain) {
                    point = point.rotateAbout(transform.pivot, transform.rotation);
                }
                transformed[index] = new Vertex(
                        point, coordinates[index][0], coordinates[index][1]
                );
            }
            output.add(new Quad(
                    transformed[0], transformed[1], transformed[2], transformed[3]
            ));
        }
    }

    private static boolean zeroSizeFace(Vec3 size, Face face) {
        if (size.x() == 0D) {
            return face.axis != Axis.X;
        }
        if (size.y() == 0D) {
            return face.axis != Axis.Y;
        }
        if (size.z() == 0D) {
            return face.axis != Axis.Z;
        }
        return false;
    }

    private static UvRect boxUv(JsonArray pair, Vec3 size, Face face) {
        if (pair.size() != 2) {
            throw new IllegalArgumentException("malformed Oritech box UV");
        }
        double u = number(pair.get(0));
        double v = number(pair.get(1));
        double x = Math.floor(size.x());
        double y = Math.floor(size.y());
        double z = Math.floor(size.z());
        return switch (face) {
            case WEST -> new UvRect(u + z + x, v + z, z, y);
            case EAST -> new UvRect(u, v + z, z, y);
            case NORTH -> new UvRect(u + z, v + z, x, y);
            case SOUTH -> new UvRect(u + z + x + z, v + z, x, y);
            case UP -> new UvRect(u + z, v, x, z);
            case DOWN -> new UvRect(u + z + x, v + z, x, -z);
        };
    }

    private static UvRect mappedUv(JsonObject mapping, Face face) {
        JsonElement element = mapping.get(face.wireName);
        if (element == null) {
            return null;
        }
        JsonObject details = object(element, "face UV");
        JsonArray uv = array(details.get("uv"), "face UV origin");
        JsonArray size = array(details.get("uv_size"), "face UV size");
        if (uv.size() != 2 || size.size() != 2) {
            throw new IllegalArgumentException("malformed Oritech face UV");
        }
        return new UvRect(
                number(uv.get(0)), number(uv.get(1)),
                number(size.get(0)), number(size.get(1))
        );
    }

    private static Vec3 signedPivot(JsonElement value) {
        Vec3 raw = vector(value, "pivot");
        return new Vec3(-raw.x() / 16D, raw.y() / 16D, raw.z() / 16D);
    }

    private static Vec3 signedRotation(JsonElement value) {
        Vec3 raw = vector(value, "rotation");
        return new Vec3(-raw.x(), -raw.y(), raw.z());
    }

    private static Vec3 vector(JsonElement value, String label) {
        JsonArray array = array(value, label);
        if (array.size() != 3) {
            throw new IllegalArgumentException(label + " must contain three numbers");
        }
        return new Vec3(number(array.get(0)), number(array.get(1)), number(array.get(2)));
    }

    private static JsonObject object(JsonElement value, String label) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonElement value, String label) {
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException(label + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("malformed Oritech GEO string");
        }
        return value.getAsString();
    }

    private static int positiveInt(JsonElement value) {
        double number = number(value);
        if (number != Math.rint(number) || number <= 0D || number > 4_096D) {
            throw new IllegalArgumentException("malformed Oritech GEO dimension");
        }
        return (int) number;
    }

    private static double number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("malformed Oritech GEO number");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || Math.abs(number) > 16_384D) {
            throw new IllegalArgumentException("Oritech GEO number is out of bounds");
        }
        return number;
    }

    private static boolean bool(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("malformed Oritech GEO boolean");
        }
        return value.getAsBoolean();
    }

    private enum Axis {
        X, Y, Z
    }

    private enum Face {
        WEST("west", Axis.X),
        EAST("east", Axis.X),
        NORTH("north", Axis.Z),
        SOUTH("south", Axis.Z),
        UP("up", Axis.Y),
        DOWN("down", Axis.Y);

        private final String wireName;
        private final Axis axis;

        Face(String wireName, Axis axis) {
            this.wireName = wireName;
            this.axis = axis;
        }
    }

    private record RawBone(
            String name,
            String parent,
            Vec3 pivot,
            Vec3 rotation,
            Boolean mirror,
            Double inflate,
            List<RawCube> cubes
    ) {
    }

    private record RawCube(
            Vec3 origin,
            Vec3 size,
            Vec3 rotation,
            Vec3 pivot,
            Double inflate,
            Boolean mirror,
            JsonElement uv
    ) {
    }

    private record UvRect(double u, double v, double width, double height) {
    }

    private record VertexSet(
            Vec3 bottomLeftBack,
            Vec3 bottomRightBack,
            Vec3 topLeftBack,
            Vec3 topRightBack,
            Vec3 topLeftFront,
            Vec3 topRightFront,
            Vec3 bottomLeftFront,
            Vec3 bottomRightFront
    ) {

        VertexSet(Vec3 origin, Vec3 size, double inflation) {
            this(
                    new Vec3(origin.x() - inflation, origin.y() - inflation,
                            origin.z() - inflation),
                    new Vec3(origin.x() - inflation, origin.y() - inflation,
                            origin.z() + size.z() + inflation),
                    new Vec3(origin.x() - inflation, origin.y() + size.y() + inflation,
                            origin.z() - inflation),
                    new Vec3(origin.x() - inflation, origin.y() + size.y() + inflation,
                            origin.z() + size.z() + inflation),
                    new Vec3(origin.x() + size.x() + inflation,
                            origin.y() + size.y() + inflation, origin.z() - inflation),
                    new Vec3(origin.x() + size.x() + inflation,
                            origin.y() + size.y() + inflation,
                            origin.z() + size.z() + inflation),
                    new Vec3(origin.x() + size.x() + inflation, origin.y() - inflation,
                            origin.z() - inflation),
                    new Vec3(origin.x() + size.x() + inflation, origin.y() - inflation,
                            origin.z() + size.z() + inflation)
            );
        }

        Vec3[] forFace(Face face, boolean boxUv, boolean mirror) {
            return switch (face) {
                case WEST -> mirror ? east() : west();
                case EAST -> mirror ? west() : east();
                case NORTH -> north();
                case SOUTH -> south();
                case UP -> mirror && !boxUv ? down() : up();
                case DOWN -> mirror && !boxUv ? up() : down();
            };
        }

        private Vec3[] west() {
            return new Vec3[]{topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
        }

        private Vec3[] east() {
            return new Vec3[]{topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
        }

        private Vec3[] north() {
            return new Vec3[]{topLeftBack, topLeftFront, bottomLeftFront, bottomLeftBack};
        }

        private Vec3[] south() {
            return new Vec3[]{topRightFront, topRightBack, bottomRightBack, bottomRightFront};
        }

        private Vec3[] up() {
            return new Vec3[]{topRightBack, topRightFront, topLeftFront, topLeftBack};
        }

        private Vec3[] down() {
            return new Vec3[]{bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};
        }
    }
}
