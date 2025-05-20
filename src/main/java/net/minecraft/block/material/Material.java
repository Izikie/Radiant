package net.minecraft.block.material;

public class Material {
    public static final Material AIR = new MaterialTransparent(MapColor.AIR_COLOR);
    public static final Material GRASS = new Material(MapColor.GRASS_COLOR);
    public static final Material GROUND = new Material(MapColor.DIRT_COLOR);
    public static final Material WOOD = (new Material(MapColor.WOOD_COLOR)).setBurning();
    public static final Material ROCK = (new Material(MapColor.STONE_COLOR)).setRequiresTool();
    public static final Material IRON = (new Material(MapColor.IRON_COLOR)).setRequiresTool();
    public static final Material ANVIL = (new Material(MapColor.IRON_COLOR)).setRequiresTool().setImmovableMobility();
    public static final Material WATER = (new MaterialLiquid(MapColor.WATER_COLOR)).setNoPushMobility();
    public static final Material LAVA = (new MaterialLiquid(MapColor.TNT_COLOR)).setNoPushMobility();
    public static final Material LEAVES = (new Material(MapColor.FOLIAGE_COLOR)).setBurning().setTranslucent().setNoPushMobility();
    public static final Material PLANTS = (new MaterialLogic(MapColor.FOLIAGE_COLOR)).setNoPushMobility();
    public static final Material VINE = (new MaterialLogic(MapColor.FOLIAGE_COLOR)).setBurning().setNoPushMobility().setReplaceable();
    public static final Material SPONGE = new Material(MapColor.YELLOW_COLOR);
    public static final Material CLOTH = (new Material(MapColor.CLOTH_COLOR)).setBurning();
    public static final Material FIRE = (new MaterialTransparent(MapColor.AIR_COLOR)).setNoPushMobility();
    public static final Material SAND = new Material(MapColor.SAND_COLOR);
    public static final Material CIRCUITS = (new MaterialLogic(MapColor.AIR_COLOR)).setNoPushMobility();
    public static final Material CARPET = (new MaterialLogic(MapColor.CLOTH_COLOR)).setBurning();
    public static final Material GLASS = (new Material(MapColor.AIR_COLOR)).setTranslucent().setAdventureModeExempt();
    public static final Material REDSTONE_LIGHT = (new Material(MapColor.AIR_COLOR)).setAdventureModeExempt();
    public static final Material TNT = (new Material(MapColor.TNT_COLOR)).setBurning().setTranslucent();
    public static final Material CORAL = (new Material(MapColor.FOLIAGE_COLOR)).setNoPushMobility();
    public static final Material ICE = (new Material(MapColor.ICE_COLOR)).setTranslucent().setAdventureModeExempt();
    public static final Material PACKED_ICE = (new Material(MapColor.ICE_COLOR)).setAdventureModeExempt();
    public static final Material SNOW = (new MaterialLogic(MapColor.SNOW_COLOR)).setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
    public static final Material CRAFTED_SNOW = (new Material(MapColor.SNOW_COLOR)).setRequiresTool();
    public static final Material CACTUS = (new Material(MapColor.FOLIAGE_COLOR)).setTranslucent().setNoPushMobility();
    public static final Material CLAY = new Material(MapColor.CLAY_COLOR);
    public static final Material GOURD = (new Material(MapColor.FOLIAGE_COLOR)).setNoPushMobility();
    public static final Material DRAGON_EGG = (new Material(MapColor.FOLIAGE_COLOR)).setNoPushMobility();
    public static final Material PORTAL = (new MaterialPortal(MapColor.AIR_COLOR)).setImmovableMobility();
    public static final Material CAKE = (new Material(MapColor.AIR_COLOR)).setNoPushMobility();
    public static final Material WEB = (new Material(MapColor.CLOTH_COLOR) {
        public boolean blocksMovement() {
            return false;
        }
    }).setRequiresTool().setNoPushMobility();
    public static final Material PISTON = (new Material(MapColor.STONE_COLOR)).setImmovableMobility();
    public static final Material BARRIER = (new Material(MapColor.AIR_COLOR)).setRequiresTool().setImmovableMobility();
    private boolean canBurn;
    private boolean replaceable;
    private boolean isTranslucent;
    private final MapColor materialMapColor;
    private boolean requiresNoTool = true;
    private int mobilityFlag;
    private boolean isAdventureModeExempt;

    public Material(MapColor color) {
        this.materialMapColor = color;
    }

    public boolean isLiquid() {
        return false;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean blocksLight() {
        return true;
    }

    public boolean blocksMovement() {
        return true;
    }

    private Material setTranslucent() {
        this.isTranslucent = true;
        return this;
    }

    protected Material setRequiresTool() {
        this.requiresNoTool = false;
        return this;
    }

    protected Material setBurning() {
        this.canBurn = true;
        return this;
    }

    public boolean getCanBurn() {
        return this.canBurn;
    }

    public Material setReplaceable() {
        this.replaceable = true;
        return this;
    }

    public boolean isReplaceable() {
        return this.replaceable;
    }

    public boolean isOpaque() {
        return !this.isTranslucent && this.blocksMovement();
    }

    public boolean isToolNotRequired() {
        return this.requiresNoTool;
    }

    public int getMaterialMobility() {
        return this.mobilityFlag;
    }

    protected Material setNoPushMobility() {
        this.mobilityFlag = 1;
        return this;
    }

    protected Material setImmovableMobility() {
        this.mobilityFlag = 2;
        return this;
    }

    protected Material setAdventureModeExempt() {
        this.isAdventureModeExempt = true;
        return this;
    }

    public MapColor getMaterialMapColor() {
        return this.materialMapColor;
    }
}
