package net.minecraft.block.material;

public class Material {
    public static final Material AIR = new MaterialTransparent(MapColor.airColor);
    public static final Material GRASS = new Material(MapColor.grassColor);
    public static final Material GROUND = new Material(MapColor.dirtColor);
    public static final Material WOOD = (new Material(MapColor.woodColor)).setBurning();
    public static final Material ROCK = (new Material(MapColor.stoneColor)).setRequiresTool();
    public static final Material IRON = (new Material(MapColor.ironColor)).setRequiresTool();
    public static final Material ANVIL = (new Material(MapColor.ironColor)).setRequiresTool().setImmovableMobility();
    public static final Material WATER = (new MaterialLiquid(MapColor.waterColor)).setNoPushMobility();
    public static final Material LAVA = (new MaterialLiquid(MapColor.tntColor)).setNoPushMobility();
    public static final Material LEAVES = (new Material(MapColor.foliageColor)).setBurning().setTranslucent().setNoPushMobility();
    public static final Material PLANTS = (new MaterialLogic(MapColor.foliageColor)).setNoPushMobility();
    public static final Material VINE = (new MaterialLogic(MapColor.foliageColor)).setBurning().setNoPushMobility().setReplaceable();
    public static final Material SPONGE = new Material(MapColor.yellowColor);
    public static final Material CLOTH = (new Material(MapColor.clothColor)).setBurning();
    public static final Material FIRE = (new MaterialTransparent(MapColor.airColor)).setNoPushMobility();
    public static final Material SAND = new Material(MapColor.sandColor);
    public static final Material CIRCUITS = (new MaterialLogic(MapColor.airColor)).setNoPushMobility();
    public static final Material CARPET = (new MaterialLogic(MapColor.clothColor)).setBurning();
    public static final Material GLASS = (new Material(MapColor.airColor)).setTranslucent().setAdventureModeExempt();
    public static final Material REDSTONE_LIGHT = (new Material(MapColor.airColor)).setAdventureModeExempt();
    public static final Material TNT = (new Material(MapColor.tntColor)).setBurning().setTranslucent();
    public static final Material CORAL = (new Material(MapColor.foliageColor)).setNoPushMobility();
    public static final Material ICE = (new Material(MapColor.iceColor)).setTranslucent().setAdventureModeExempt();
    public static final Material PACKED_ICE = (new Material(MapColor.iceColor)).setAdventureModeExempt();
    public static final Material SNOW = (new MaterialLogic(MapColor.snowColor)).setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
    public static final Material CRAFTED_SNOW = (new Material(MapColor.snowColor)).setRequiresTool();
    public static final Material CACTUS = (new Material(MapColor.foliageColor)).setTranslucent().setNoPushMobility();
    public static final Material CLAY = new Material(MapColor.clayColor);
    public static final Material GOURD = (new Material(MapColor.foliageColor)).setNoPushMobility();
    public static final Material DRAGON_EGG = (new Material(MapColor.foliageColor)).setNoPushMobility();
    public static final Material PORTAL = (new MaterialPortal(MapColor.airColor)).setImmovableMobility();
    public static final Material CAKE = (new Material(MapColor.airColor)).setNoPushMobility();
    public static final Material WEB = (new Material(MapColor.clothColor) {
        public boolean blocksMovement() {
            return false;
        }
    }).setRequiresTool().setNoPushMobility();
    public static final Material PISTON = (new Material(MapColor.stoneColor)).setImmovableMobility();
    public static final Material BARRIER = (new Material(MapColor.airColor)).setRequiresTool().setImmovableMobility();
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
        return this.isTranslucent ? false : this.blocksMovement();
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
