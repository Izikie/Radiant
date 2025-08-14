package net.minecraft.item.creativetab;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public abstract class CreativeTabs {
    public static final CreativeTabs[] CREATIVE_TABS = new CreativeTabs[12];
    public static final CreativeTabs TAB_BLOCK = new CreativeTabs(0, "buildingBlocks") {
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.BRICK_BLOCK);
        }
    };
    public static final CreativeTabs TAB_DECORATIONS = new CreativeTabs(1, "decorations") {
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.DOUBLE_PLANT);
        }

        public int getIconItemDamage() {
            return BlockDoublePlant.EnumPlantType.PAEONIA.getMeta();
        }
    };
    public static final CreativeTabs TAB_REDSTONE = new CreativeTabs(2, "redstone") {
        public Item getTabIconItem() {
            return Items.REDSTONE;
        }
    };
    public static final CreativeTabs TAB_TRANSPORT = new CreativeTabs(3, "transportation") {
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.GOLDEN_RAIL);
        }
    };
    public static final CreativeTabs TAB_MISC = (new CreativeTabs(4, "misc") {
        public Item getTabIconItem() {
            return Items.LAVA_BUCKET;
        }
    }).setRelevantEnchantmentTypes(EnchantmentTarget.ALL);
    public static final CreativeTabs TAB_ALL_SEARCH = (new CreativeTabs(5, "search") {
        public Item getTabIconItem() {
            return Items.COMPASS;
        }
    }).setBackgroundImageName("item_search.png");
    public static final CreativeTabs TAB_FOOD = new CreativeTabs(6, "food") {
        public Item getTabIconItem() {
            return Items.APPLE;
        }
    };
    public static final CreativeTabs TAB_TOOLS = (new CreativeTabs(7, "tools") {
        public Item getTabIconItem() {
            return Items.IRON_AXE;
        }
    }).setRelevantEnchantmentTypes(EnchantmentTarget.DIGGER, EnchantmentTarget.FISHING_ROD, EnchantmentTarget.BREAKABLE);
    public static final CreativeTabs TAB_COMBAT = (new CreativeTabs(8, "combat") {
        public Item getTabIconItem() {
            return Items.GOLDEN_SWORD;
        }
    }).setRelevantEnchantmentTypes(EnchantmentTarget.ARMOR, EnchantmentTarget.ARMOR_FEET, EnchantmentTarget.ARMOR_HEAD, EnchantmentTarget.ARMOR_LEGS, EnchantmentTarget.ARMOR_TORSO, EnchantmentTarget.BOW, EnchantmentTarget.WEAPON);
    public static final CreativeTabs TAB_BREWING = new CreativeTabs(9, "brewing") {
        public Item getTabIconItem() {
            return Items.POTION;
        }
    };
    public static final CreativeTabs TAB_MATERIALS = new CreativeTabs(10, "materials") {
        public Item getTabIconItem() {
            return Items.STICK;
        }
    };
    public static final CreativeTabs TAB_INVENTORY = (new CreativeTabs(11, "inventory") {
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.CHEST);
        }
    }).setBackgroundImageName("inventory.png").setNoScrollbar().setNoTitle();
    private final int tabIndex;
    private final String tabLabel;
    private String theTexture = "items.png";
    private boolean hasScrollbar = true;
    private boolean drawTitle = true;
    private EnchantmentTarget[] enchantmentTypes;
    private ItemStack iconItemStack;

    public CreativeTabs(int index, String label) {
        this.tabIndex = index;
        this.tabLabel = label;
        CREATIVE_TABS[index] = this;
    }

    public int getTabIndex() {
        return this.tabIndex;
    }

    public String getTabLabel() {
        return this.tabLabel;
    }

    public String getTranslatedTabLabel() {
        return "itemGroup." + this.getTabLabel();
    }

    public ItemStack getIconItemStack() {
        if (this.iconItemStack == null) {
            this.iconItemStack = new ItemStack(this.getTabIconItem(), 1, this.getIconItemDamage());
        }

        return this.iconItemStack;
    }

    public abstract Item getTabIconItem();

    public int getIconItemDamage() {
        return 0;
    }

    public String getBackgroundImageName() {
        return this.theTexture;
    }

    public CreativeTabs setBackgroundImageName(String texture) {
        this.theTexture = texture;
        return this;
    }

    public boolean drawInForegroundOfTab() {
        return this.drawTitle;
    }

    public CreativeTabs setNoTitle() {
        this.drawTitle = false;
        return this;
    }

    public boolean shouldHidePlayerInventory() {
        return this.hasScrollbar;
    }

    public CreativeTabs setNoScrollbar() {
        this.hasScrollbar = false;
        return this;
    }

    public int getTabColumn() {
        return this.tabIndex % 6;
    }

    public boolean isTabInFirstRow() {
        return this.tabIndex < 6;
    }

    public EnchantmentTarget[] getRelevantEnchantmentTypes() {
        return this.enchantmentTypes;
    }

    public CreativeTabs setRelevantEnchantmentTypes(EnchantmentTarget... types) {
        this.enchantmentTypes = types;
        return this;
    }

    public boolean hasRelevantEnchantmentType(EnchantmentTarget enchantmentType) {
        if (this.enchantmentTypes != null) {
            for (EnchantmentTarget enumenchantmenttype : this.enchantmentTypes) {
                if (enumenchantmenttype == enchantmentType) {
                    return true;
                }
            }

        }
        return false;
    }

    public void displayAllReleventItems(List<ItemStack> p_78018_1_) {
        for (Item item : Item.itemRegistry) {
            if (item != null && item.getCreativeTab() == this) {
                item.getSubItems(item, this, p_78018_1_);
            }
        }

        if (this.getRelevantEnchantmentTypes() != null) {
            this.addEnchantmentBooksToList(p_78018_1_, this.getRelevantEnchantmentTypes());
        }
    }

    public void addEnchantmentBooksToList(List<ItemStack> itemList, EnchantmentTarget... enchantmentTypes) {
        for (Enchantment enchantment : Enchantment.ENCHANTMENTS_BOOK_LIST) {
            if (enchantment == null || enchantment.type == null)
                continue;

            for (EnchantmentTarget target : enchantmentTypes) {
                if (enchantment.type == target) {
                    itemList.add(Items.ENCHANTED_BOOK.getEnchantedItemStack(new EnchantmentData(enchantment, enchantment.getMaxLevel())));
                    break;
                }
            }
        }
    }
}
