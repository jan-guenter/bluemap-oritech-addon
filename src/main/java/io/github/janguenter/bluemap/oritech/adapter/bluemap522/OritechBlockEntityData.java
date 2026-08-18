/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;

/** Retains only Oritech's persistent machine paint ordinal. */
public final class OritechBlockEntityData extends MCABlockEntity {

    private Object color;

    public OritechBlockEntityData() {
    }

    int colorOrdinal() {
        if (color instanceof Number number) {
            int value = number.intValue();
            return value >= 0 && value <= 8 ? value : 0;
        }
        return 0;
    }
}
