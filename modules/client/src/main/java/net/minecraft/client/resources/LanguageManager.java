package net.minecraft.client.resources;

import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.util.StringTranslate;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

public class LanguageManager implements IResourceManagerReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageManager.class);
    private final IMetadataSerializer theMetadataSerializer;
    private String currentLanguage;
    protected static final Locale CURRENT_LOCALE = new Locale();
    private final Map<String, Language> languageMap = new HashMap<>();

    public LanguageManager(IMetadataSerializer theMetadataSerializerIn, String currentLanguageIn) {
        this.theMetadataSerializer = theMetadataSerializerIn;
        this.currentLanguage = currentLanguageIn;
        I18n.setLocale(CURRENT_LOCALE);
    }

    public void parseLanguageMetadata(List<IResourcePack> resourcesPacks) {
        this.languageMap.clear();

        for (IResourcePack iresourcepack : resourcesPacks) {
            try {
                LanguageMetadataSection languagemetadatasection = iresourcepack.getPackMetadata(this.theMetadataSerializer, "language");

                if (languagemetadatasection != null) {
                    for (Language language : languagemetadatasection.getLanguages()) {
                        if (!this.languageMap.containsKey(language.getLanguageCode())) {
                            this.languageMap.put(language.getLanguageCode(), language);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Unable to parse metadata section of resourcepack: {}", iresourcepack.getPackName(), exception);
            } catch (IOException exception) {
                LOGGER.warn("Unable to parse metadata section of resourcepack: {}", iresourcepack.getPackName(), exception);
            }
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        List<String> list = new ArrayList<>(List.of("en_US"));
    
        if (!"en_US".equals(this.currentLanguage)) {
            list.add(this.currentLanguage);
        }

        CURRENT_LOCALE.loadLocaleDataFiles(resourceManager, list);
        StringTranslate.replaceWith(CURRENT_LOCALE.properties);
    }

    public boolean isCurrentLocaleUnicode() {
        return CURRENT_LOCALE.isUnicode();
    }

    public boolean isCurrentLanguageBidirectional() {
        return this.getCurrentLanguage() != null && this.getCurrentLanguage().isBidirectional();
    }

    public void setCurrentLanguage(Language currentLanguageIn) {
        this.currentLanguage = currentLanguageIn.getLanguageCode();
    }

    public Language getCurrentLanguage() {
        return this.languageMap.containsKey(this.currentLanguage) ? this.languageMap.get(this.currentLanguage) : this.languageMap.get("en_US");
    }

    public SortedSet<Language> getLanguages() {
        return new TreeSet<>(this.languageMap.values());
    }
}
