package com.supermartijn642.simplemagnets.gui;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.gui.ScreenUtils;
import com.supermartijn642.core.gui.TileEntityBaseContainerScreen;
import com.supermartijn642.simplemagnets.DemagnetizationCoilTile;
import net.minecraft.util.ResourceLocation;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
public abstract class BaseDemagnetizationCoilContainerScreen<T extends BaseDemagnetizationCoilContainer> extends TileEntityBaseContainerScreen<DemagnetizationCoilTile,T> {

    public BaseDemagnetizationCoilContainerScreen(T container, String title){
        super(container, TextComponents.translation(title).get());
    }

    @Override
    protected int sizeX(DemagnetizationCoilTile demagnetizationCoilTile){
        return this.menu.width;
    }

    @Override
    protected int sizeY(DemagnetizationCoilTile demagnetizationCoilTile){
        return this.menu.height;
    }

    @Override
    public void tick(){
        DemagnetizationCoilTile tile = this.getObjectOrClose();
        if(tile == null)
            return;

        super.tick();
        this.tick(tile);
    }

    protected abstract void tick(DemagnetizationCoilTile tile);

    protected abstract String getBackground();

    @Override
    protected void renderBackground(int mouseX, int mouseY, DemagnetizationCoilTile object){
        ScreenUtils.bindTexture(new ResourceLocation("simplemagnets", "textures/" + this.getBackground()));
        ScreenUtils.drawTexture(0, 0, this.sizeX(), this.sizeY());
    }
}
