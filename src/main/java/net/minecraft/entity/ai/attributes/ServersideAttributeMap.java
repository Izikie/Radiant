package net.minecraft.entity.ai.attributes;

import net.minecraft.server.management.LowerStringMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServersideAttributeMap extends BaseAttributeMap {
    private final Set<IAttributeInstance> attributeInstanceSet = new HashSet<>();
    protected final Map<String, IAttributeInstance> descriptionToAttributeInstanceMap = new LowerStringMap<>();

    @Override
    public ModifiableAttributeInstance getAttributeInstance(IAttribute attribute) {
        return (ModifiableAttributeInstance) super.getAttributeInstance(attribute);
    }

    @Override
    public ModifiableAttributeInstance getAttributeInstanceByName(String attributeName) {
        IAttributeInstance iattributeinstance = super.getAttributeInstanceByName(attributeName);

        if (iattributeinstance == null) {
            iattributeinstance = this.descriptionToAttributeInstanceMap.get(attributeName);
        }

        return (ModifiableAttributeInstance) iattributeinstance;
    }

    @Override
    public IAttributeInstance registerAttribute(IAttribute attribute) {
        IAttributeInstance iattributeinstance = super.registerAttribute(attribute);

        if (attribute instanceof RangedAttribute rangedAttribute && rangedAttribute.getDescription() != null) {
            this.descriptionToAttributeInstanceMap.put(rangedAttribute.getDescription(), iattributeinstance);
        }

        return iattributeinstance;
    }

    @Override
    protected IAttributeInstance func_180376_c(IAttribute attribute) {
        return new ModifiableAttributeInstance(this, attribute);
    }

    @Override
    public void func_180794_a(IAttributeInstance instance) {
        if (instance.getAttribute().getShouldWatch()) {
            this.attributeInstanceSet.add(instance);
        }

        for (IAttribute iattribute : this.field_180377_c.get(instance.getAttribute())) {
            ModifiableAttributeInstance modifiableattributeinstance = this.getAttributeInstance(iattribute);

            if (modifiableattributeinstance != null) {
                modifiableattributeinstance.flagForUpdate();
            }
        }
    }

    public Set<IAttributeInstance> getAttributeInstanceSet() {
        return this.attributeInstanceSet;
    }

    public Collection<IAttributeInstance> getWatchedAttributes() {
        Set<IAttributeInstance> set = new HashSet<>();

        for (IAttributeInstance iattributeinstance : this.getAllAttributes()) {
            if (iattributeinstance.getAttribute().getShouldWatch()) {
                set.add(iattributeinstance);
            }
        }

        return set;
    }
}
