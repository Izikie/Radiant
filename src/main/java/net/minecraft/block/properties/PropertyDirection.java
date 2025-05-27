package net.minecraft.block.properties;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import net.minecraft.util.Direction;

import java.util.Collection;
import java.util.List;

public class PropertyDirection extends PropertyEnum<Direction> {
    protected PropertyDirection(String name, Collection<Direction> values) {
        super(name, Direction.class, values);
    }

    public static PropertyDirection create(String name) {
        return create(name, Predicates.alwaysTrue());
    }

    public static PropertyDirection create(String name, Predicate<Direction> filter) {
        return create(name, Collections2.filter(List.of(Direction.values()), filter));
    }

    public static PropertyDirection create(String name, Collection<Direction> values) {
        return new PropertyDirection(name, values);
    }
}
