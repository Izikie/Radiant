package net.minecraft.block.state;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.util.collection.Cartesian;
import net.minecraft.util.collection.MapPopulator;

import java.util.*;

public class BlockState {
    private static final Function<IProperty<?>, String> GET_NAME_FUNC = p_apply_1_ -> p_apply_1_ == null ? "<NULL>" : p_apply_1_.getName();
    private final Block block;
    private final ImmutableList<IProperty<?>> properties;
    private final ImmutableList<IBlockState> validStates;

    public BlockState(Block blockIn, IProperty<?>... properties) {
        this.block = blockIn;
        Arrays.sort(properties, Comparator.comparing(IProperty::getName));
        this.properties = ImmutableList.copyOf(properties);
        Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> map = new LinkedHashMap<>();
        List<StateImplementation> list = new ArrayList<>();

        for (List<Comparable<?>> list1 : Cartesian.cartesianProduct(this.getAllowedValues())) {
            Map<IProperty<?>, Comparable<?>> map1 = MapPopulator.createMap(this.properties, list1);
            StateImplementation blockstate$stateimplementation = new StateImplementation(blockIn, ImmutableMap.copyOf(map1));
            map.put(map1, blockstate$stateimplementation);
            list.add(blockstate$stateimplementation);
        }

        for (StateImplementation blockstate$stateimplementation1 : list) {
            blockstate$stateimplementation1.buildPropertyValueTable(map);
        }

        this.validStates = ImmutableList.copyOf(list);
    }

    public ImmutableList<IBlockState> getValidStates() {
        return this.validStates;
    }

    private List<Iterable<? extends Comparable<?>>> getAllowedValues() {
        List<Iterable<? extends Comparable<?>>> list = new ArrayList<>();

        for (int i = 0; i < this.properties.size(); ++i) {
            list.add(this.properties.get(i).getAllowedValues());
        }

        return list;
    }

    public IBlockState getBaseState() {
        return this.validStates.getFirst();
    }

    public Block getBlock() {
        return this.block;
    }

    public Collection<IProperty<?>> getProperties() {
        return this.properties;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("block", Block.blockRegistry.getNameForObject(this.block)).add("properties", Iterables.transform(this.properties, GET_NAME_FUNC)).toString();
    }

    static class StateImplementation extends BlockStateBase {
        private final Block block;
        private final ImmutableMap<IProperty<?>, Comparable<?>> properties;
        private ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> propertyValueTable;

        private StateImplementation(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
            this.block = blockIn;
            this.properties = propertiesIn;
        }

        @Override
        public Collection<IProperty<?>> getPropertyNames() {
            return Collections.unmodifiableCollection(this.properties.keySet());
        }

        @Override
        public <T extends Comparable<T>> T getValue(IProperty<T> property) {
            if (!this.properties.containsKey(property)) {
                throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.block.getBlockState());
            } else {
                return (T) this.properties.get(property);
            }
        }

        @Override
        public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
            // BUGFIX: Crash from the value not being included in the properties allowed values
            try {
                if (!this.properties.containsKey(property)) {
                    throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.block.getBlockState());
                } else if (!property.getAllowedValues().contains(value)) {
                    throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on block " + Block.blockRegistry.getNameForObject(this.block) + ", it is not an allowed value");
                } else {
                    return this.properties.get(property) == value ? this : this.propertyValueTable.get(property, value);
                }
            } catch (IllegalArgumentException _) {
                return this;
            }
        }

        @Override
        public ImmutableMap<IProperty<?>, Comparable<?>> getProperties() {
            return this.properties;
        }

        @Override
        public Block getBlock() {
            return this.block;
        }

	    public int hashCode() {
            return this.properties.hashCode();
        }

        public void buildPropertyValueTable(Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> map) {
            if (this.propertyValueTable != null) {
                throw new IllegalStateException();
            }

            Table<IProperty<?>, Comparable<?>, IBlockState> table = HashBasedTable.create();

            for (Map.Entry<IProperty<?>, Comparable<?>> entry : this.properties.entrySet()) {
                IProperty<?> property = entry.getKey();
                for (Comparable<?> value : property.getAllowedValues()) {
                    if (!value.equals(entry.getValue())) {
                        table.put(property, value, map.get(this.getPropertiesWithValue(property, value)));
                    }
                }
            }

            this.propertyValueTable = ImmutableTable.copyOf(table);
        }

        private Map<IProperty<?>, Comparable<?>> getPropertiesWithValue(IProperty<?> property, Comparable<?> value) {
            Map<IProperty<?>, Comparable<?>> newMap = new HashMap<>(this.properties);
            newMap.put(property, value);
            return newMap;
        }
    }
}
