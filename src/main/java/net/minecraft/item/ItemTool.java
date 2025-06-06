package net.minecraft.item;

import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public class ItemTool extends Item {
    private final Set<Block> effectiveBlocks;
    protected final float efficiencyOnProperMaterial;
    private final float damageVsEntity;
    protected final ToolMaterial toolMaterial;

    protected ItemTool(float attackDamage, ToolMaterial material, Set<Block> effectiveBlocks) {
        this.toolMaterial = material;
        this.effectiveBlocks = effectiveBlocks;
        this.maxStackSize = 1;
        this.setMaxDamage(material.getMaxUses());
        this.efficiencyOnProperMaterial = material.getEfficiencyOnProperMaterial();
        this.damageVsEntity = attackDamage + material.getDamageVsEntity();
        this.setCreativeTab(CreativeTabs.TAB_TOOLS);
    }

    public float getStrVsBlock(ItemStack stack, Block state) {
        return this.effectiveBlocks.contains(state) ? this.efficiencyOnProperMaterial : 1.0F;
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(2, attacker);
        return true;
    }

    public boolean onBlockDestroyed(ItemStack stack, World worldIn, Block blockIn, BlockPos pos, EntityLivingBase playerIn) {
        if (blockIn.getBlockHardness(worldIn, pos) != 0.0D) {
            stack.damageItem(1, playerIn);
        }

        return true;
    }

    public boolean isFull3D() {
        return true;
    }

    public ToolMaterial getToolMaterial() {
        return this.toolMaterial;
    }

    public int getItemEnchantability() {
        return this.toolMaterial.getEnchantability();
    }

    public String getToolMaterialName() {
        return this.toolMaterial.toString();
    }

    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return this.toolMaterial.getRepairItem() == repair.getItem() || super.getIsRepairable(toRepair, repair);
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers() {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers();
        multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getAttributeUnlocalizedName(), new AttributeModifier(ITEM_MODIFIER_UUID, "Tool modifier", this.damageVsEntity, 0));
        return multimap;
    }
}
