package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.CustomColors;
import net.optifine.render.GlBlendState;
import net.optifine.util.FontUtils;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

public class FontRenderer implements IResourceManagerReloadListener {
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String CHARACTER_DICTIONARY = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

    private static final ResourceLocation[] UNICODE_PAGE_LOCATIONS = new ResourceLocation[256];
    private final int[] charWidth = new int[256];
    public final int FONT_HEIGHT = 9;
    public final Random fontRandom = new Random();
    private final byte[] glyphWidth = new byte[65536];
    private final int[] colorCode = new int[32];
    private ResourceLocation locationFontTexture;
    private final TextureManager renderEngine;
    private float posX;
    private float posY;
    private boolean unicodeFlag;
    private boolean bidiFlag;
    private float red;
    private float blue;
    private float green;
    private float alpha;
    private int textColor;
    private boolean randomStyle;
    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;
    public final GameSettings gameSettings;
    public final ResourceLocation locationFontTextureBase;
    public float offsetBold = 1.0F;
    private final float[] charWidthFloat = new float[256];
    private boolean blend = false;
    private final GlBlendState oldBlendState = new GlBlendState();

    public FontRenderer(GameSettings gameSettingsIn, ResourceLocation location, TextureManager textureManagerIn, boolean unicode) {
        this.gameSettings = gameSettingsIn;
        this.locationFontTextureBase = location;
        this.locationFontTexture = location;
        this.renderEngine = textureManagerIn;
        this.unicodeFlag = unicode;
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);
        this.bindTexture(this.locationFontTexture);

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) {
                k += 85;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }

        this.readGlyphSizes();
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);

        Arrays.fill(UNICODE_PAGE_LOCATIONS, null);

        this.readFontTexture();
        this.readGlyphSizes();
    }

    private void readFontTexture() {
        BufferedImage bufferedimage;

        try {
            bufferedimage = TextureUtil.readBufferedImage(this.getResourceInputStream(this.locationFontTexture));
        } catch (IOException ioexception1) {
            throw new RuntimeException(ioexception1);
        }

        Properties properties = FontUtils.readFontProperties(this.locationFontTexture);
        this.blend = FontUtils.readBoolean(properties, "blend", false);
        int i = bufferedimage.getWidth();
        int j = bufferedimage.getHeight();
        int k = i / 16;
        int l = j / 16;
        float f = i / 128.0F;
        float f1 = Config.limit(f, 1.0F, 2.0F);
        this.offsetBold = 1.0F / f1;
        float f2 = FontUtils.readFloat(properties, "offsetBold", -1.0F);

        if (f2 >= 0.0F) {
            this.offsetBold = f2;
        }

        int[] aint = new int[i * j];
        bufferedimage.getRGB(0, 0, i, j, aint, 0, i);

        for (int i1 = 0; i1 < 256; ++i1) {
            int j1 = i1 % 16;
            int k1 = i1 / 16;
            int l1;

            for (l1 = k - 1; l1 >= 0; --l1) {
                int i2 = j1 * k + l1;
                boolean flag = true;

                for (int j2 = 0; j2 < l; ++j2) {
                    int k2 = (k1 * l + j2) * i;
                    int l2 = aint[i2 + k2];
                    int i3 = l2 >> 24 & 255;

                    if (i3 > 16) {
                        flag = false;
                        break;
                    }
                }

                if (!flag) {
                    break;
                }
            }

            if (i1 == 32) {
                if (k <= 8) {
                    l1 = (int) (2.0F * f);
                } else {
                    l1 = (int) (1.5F * f);
                }
            }

            this.charWidthFloat[i1] = (l1 + 1) / f + 1.0F;
        }

        FontUtils.readCustomCharWidths(properties, this.charWidthFloat);

        for (int j3 = 0; j3 < this.charWidth.length; ++j3) {
            this.charWidth[j3] = Math.round(this.charWidthFloat[j3]);
        }
    }

    private void readGlyphSizes() {
        InputStream inputstream = null;

        try {
            inputstream = this.getResourceInputStream(new ResourceLocation("font/glyph_sizes.bin"));
            inputstream.read(this.glyphWidth);
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        } finally {
            IOUtils.closeQuietly(inputstream);
        }
    }

    private float renderChar(char ch, boolean italic) {
        if (ch == 32 || ch == 160) {
            return this.unicodeFlag ? 4.0F : this.charWidthFloat[ch];
        } else {
            int charIndex = CHARACTER_DICTIONARY.indexOf(ch);
            return charIndex != -1 && !this.unicodeFlag ? this.renderDefaultChar(charIndex, italic) : this.renderUnicodeChar(ch, italic);
        }
    }

    private float renderDefaultChar(int ch, boolean italic) {
        int i = ch % 16 * 8;
        int j = ch / 16 * 8;

        int italicStyle = italic ? 1 : 0;
        this.bindTexture(this.locationFontTexture);
        float charWidth = this.charWidthFloat[ch];

        float f1 = 7.99F;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        GL11.glTexCoord2f(i / 128.0F, j / 128.0F);
        GL11.glVertex3f(this.posX + italicStyle, this.posY, 0.0F);

        GL11.glTexCoord2f(i / 128.0F, (j + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX - italicStyle, this.posY + 7.99F, 0.0F);

        GL11.glTexCoord2f((i + f1 - 1.0F) / 128.0F, j / 128.0F);
        GL11.glVertex3f(this.posX + f1 - 1.0F + italicStyle, this.posY, 0.0F);

        GL11.glTexCoord2f((i + f1 - 1.0F) / 128.0F, (j + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX + f1 - 1.0F - italicStyle, this.posY + 7.99F, 0.0F);

        GL11.glEnd();
        return charWidth;
    }

    private ResourceLocation getUnicodePageLocation(int page) {
        if (UNICODE_PAGE_LOCATIONS[page] == null) {
            UNICODE_PAGE_LOCATIONS[page] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page));
            UNICODE_PAGE_LOCATIONS[page] = FontUtils.getHdFontLocation(UNICODE_PAGE_LOCATIONS[page]);
        }

        return UNICODE_PAGE_LOCATIONS[page];
    }

    private void loadGlyphTexture(int page) {
        this.bindTexture(this.getUnicodePageLocation(page));
    }

    private float renderUnicodeChar(char ch, boolean italic) {
        if (this.glyphWidth[ch] == 0) {
            return 0.0F;
        } else {
            int i = ch / 256;
            this.loadGlyphTexture(i);
            float glyphX = this.glyphWidth[ch] >>> 4;
            int glyphY = this.glyphWidth[ch] & 15;
            float modifiedY = glyphY + 1;
            float f2 = (ch % 16 * 16) + glyphX;
            float f3 = ((ch & 255) / 16 * 16);
            float combinedGlyphSize = modifiedY - glyphX - 0.02F;
            float italicStyle = italic ? 1.0F : 0.0F;

            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

            GL11.glTexCoord2f(f2 / 256.0F, f3 / 256.0F);
            GL11.glVertex3f(this.posX + italicStyle, this.posY, 0.0F);

            GL11.glTexCoord2f(f2 / 256.0F, (f3 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX - italicStyle, this.posY + 7.99F, 0.0F);

            GL11.glTexCoord2f((f2 + combinedGlyphSize) / 256.0F, f3 / 256.0F);
            GL11.glVertex3f(this.posX + combinedGlyphSize / 2.0F + italicStyle, this.posY, 0.0F);

            GL11.glTexCoord2f((f2 + combinedGlyphSize) / 256.0F, (f3 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX + combinedGlyphSize / 2.0F - italicStyle, this.posY + 7.99F, 0.0F);

            GL11.glEnd();
            return (modifiedY - glyphX) / 2.0F + 1.0F;
        }
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, x, y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        GlStateManager.enableAlpha();

        if (this.blend) {
            GlStateManager.getBlendState(this.oldBlendState);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
        }

        this.resetStyles();
        int i;

        if (dropShadow) {
            i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
            i = Math.max(i, this.renderString(text, x, y, color, false));
        } else {
            i = this.renderString(text, x, y, color, false);
        }

        if (this.blend) {
            GlStateManager.setBlendState(this.oldBlendState);
        }

        return i;
    }

    private String bidiReorder(String text) {
        try {
            Bidi bidi = new Bidi((new ArabicShaping(8)).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException var3) {
            return text;
        }
    }

    private void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private void renderStringAtPos(String text, boolean shadow) {
        for (int messageChar = 0; messageChar < text.length(); ++messageChar) {
            char letter = text.charAt(messageChar);

            if (letter == 167 && messageChar + 1 < text.length()) {
                int styleIndex = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(messageChar + 1));

                if (styleIndex < 16) {
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    this.randomStyle = false;
                    this.boldStyle = false;

                    if (styleIndex < 0) {
                        styleIndex = 15;
                    }

                    if (shadow) {
                        styleIndex += 16;
                    }

                    int currentColor = this.colorCode[styleIndex];

                    if (Config.isCustomColors()) {
                        currentColor = CustomColors.getTextColor(styleIndex, currentColor);
                    }

                    this.textColor = currentColor;
                    GlStateManager.color((currentColor >> 16) / 255.0F, (currentColor >> 8 & 255) / 255.0F, (currentColor & 255) / 255.0F, alpha);
                } else if (styleIndex == 16) {
                    this.randomStyle = true;
                } else if (styleIndex == 17) {
                    this.boldStyle = true;
                } else if (styleIndex == 18) {
                    this.strikethroughStyle = true;
                } else if (styleIndex == 19) {
                    this.underlineStyle = true;
                } else if (styleIndex == 20) {
                    this.italicStyle = true;
                } else {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    GlStateManager.color(red, blue, green, alpha);
                }

                ++messageChar;
            } else {
                int index = CHARACTER_DICTIONARY.indexOf(letter);

                if (this.randomStyle && index != -1) {
                    int charWidth = this.getCharWidth(letter);
                    char charIndex;

                    do {
                        index = this.fontRandom.nextInt(CHARACTER_DICTIONARY.length());
                        charIndex = CHARACTER_DICTIONARY.charAt(index);

                    } while (charWidth != this.getCharWidth(charIndex));

                    letter = charIndex;
                }

                float boldWidth = index == -1 || this.unicodeFlag ? 0.5F : this.offsetBold;
                boolean small = (letter == 0 || index == -1 || this.unicodeFlag) && shadow;

                if (small) {
                    this.posX -= boldWidth;
                    this.posY -= boldWidth;
                }

                float effectiveWidth = this.renderChar(letter, this.italicStyle);

                if (small) {
                    this.posX += boldWidth;
                    this.posY += boldWidth;
                }

                if (this.boldStyle) {
                    this.posX += boldWidth;

                    if (small) {
                        this.posX -= boldWidth;
                        this.posY -= boldWidth;
                    }

                    this.renderChar(letter, this.italicStyle);
                    this.posX -= boldWidth;

                    if (small) {
                        this.posX += boldWidth;
                        this.posY += boldWidth;
                    }

                    effectiveWidth += boldWidth;
                }

                this.doDraw(effectiveWidth);
            }
        }
    }

    protected void doDraw(float p_doDraw_1_) {
        if (this.strikethroughStyle) {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION);
            worldRenderer.pos(this.posX, (this.posY + (this.FONT_HEIGHT / 2F)), 0.0D).endVertex();
            worldRenderer.pos((this.posX + p_doDraw_1_), (this.posY + (this.FONT_HEIGHT / 2F)), 0.0D).endVertex();
            worldRenderer.pos((this.posX + p_doDraw_1_), (this.posY + (this.FONT_HEIGHT / 2F) - 1.0F), 0.0D).endVertex();
            worldRenderer.pos(this.posX, (this.posY + (this.FONT_HEIGHT / 2F) - 1.0F), 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        if (this.underlineStyle) {
            int i = this.underlineStyle ? -1 : 0;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION);
            worldRenderer.pos((this.posX + i), (this.posY + this.FONT_HEIGHT), 0.0D).endVertex();
            worldRenderer.pos((this.posX + p_doDraw_1_), (this.posY + this.FONT_HEIGHT), 0.0D).endVertex();
            worldRenderer.pos((this.posX + p_doDraw_1_), (this.posY + this.FONT_HEIGHT - 1.0F), 0.0D).endVertex();
            worldRenderer.pos((this.posX + i), (this.posY + this.FONT_HEIGHT - 1.0F), 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        this.posX += p_doDraw_1_;
    }

    private int renderStringAligned(String text, int x, int y, int width, int color, boolean dropShadow) {
        if (this.bidiFlag) {
            int i = this.getStringWidth(this.bidiReorder(text));
            x = x + width - i;
        }

        return this.renderString(text, x, y, color, dropShadow);
    }

    private int renderString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) {
            return 0;
        } else {
            if (this.bidiFlag) {
                text = this.bidiReorder(text);
            }

            if ((color & -67108864) == 0) {
                color |= -16777216;
            }

            if (dropShadow) {
                color = (color & 16579836) >> 2 | color & -16777216;
            }

            this.red = (color >> 16 & 255) / 255.0F;
            this.blue = (color >> 8 & 255) / 255.0F;
            this.green = (color & 255) / 255.0F;
            this.alpha = (color >> 24 & 255) / 255.0F;
            GlStateManager.color(red, blue, green, alpha);
            this.posX = x;
            this.posY = y;
            this.renderStringAtPos(text, dropShadow);
            return (int) this.posX;
        }
    }

    public int getStringWidth(String text) {
        if (text == null) {
            return 0;
        } else {
            float width = 0.0F;
            boolean bold = false;

            for (int messageChar = 0; messageChar < text.length(); ++messageChar) {
                char character = text.charAt(messageChar);
                float characterWidth = this.getCharWidthFloat(character);

                if (characterWidth < 0.0F && messageChar < text.length() - 1) {
                    ++messageChar;
                    character = text.charAt(messageChar);

                    if (character != 108 && character != 76) {
                        if (character == 114 || character == 82) {
                            bold = false;
                        }
                    } else {
                        bold = true;
                    }

                    characterWidth = 0.0F;
                }

                width += characterWidth;

                if (bold && characterWidth > 0.0F) {
                    width += this.unicodeFlag ? 1.0F : this.offsetBold;
                }
            }

            return Math.round(width);
        }
    }

    public int getCharWidth(char character) {
        return Math.round(this.getCharWidthFloat(character));
    }

    private float getCharWidthFloat(char p_getCharWidthFloat_1_) {
        if (p_getCharWidthFloat_1_ == 167) {
            return -1.0F;
        } else if (p_getCharWidthFloat_1_ != 32 && p_getCharWidthFloat_1_ != 160) {
            int i = CHARACTER_DICTIONARY.indexOf(p_getCharWidthFloat_1_);

            if (p_getCharWidthFloat_1_ > 0 && i != -1 && !this.unicodeFlag) {
                return this.charWidthFloat[i];
            } else if (this.glyphWidth[p_getCharWidthFloat_1_] != 0) {
                int j = this.glyphWidth[p_getCharWidthFloat_1_] >>> 4;
                int k = this.glyphWidth[p_getCharWidthFloat_1_] & 15;

                if (k > 7) {
                    k = 15;
                    j = 0;
                }

                ++k;
                return ((k - j) / 2 + 1);
            } else {
                return 0.0F;
            }
        } else {
            return this.charWidthFloat[32];
        }
    }

    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        StringBuilder stringbuilder = new StringBuilder();
        float f = 0.0F;
        int i = reverse ? text.length() - 1 : 0;
        int j = reverse ? -1 : 1;
        boolean flag = false;
        boolean flag1 = false;

        for (int k = i; k >= 0 && k < text.length() && f < width; k += j) {
            char c0 = text.charAt(k);
            float f1 = this.getCharWidthFloat(c0);

            if (flag) {
                flag = false;

                if (c0 != 108 && c0 != 76) {
                    if (c0 == 114 || c0 == 82) {
                        flag1 = false;
                    }
                } else {
                    flag1 = true;
                }
            } else if (f1 < 0.0F) {
                flag = true;
            } else {
                f += f1;

                if (flag1) {
                    ++f;
                }
            }

            if (f > width) {
                break;
            }

            if (reverse) {
                stringbuilder.insert(0, c0);
            } else {
                stringbuilder.append(c0);
            }
        }

        return stringbuilder.toString();
    }

    private String trimStringNewline(String text) {
        while (text != null && text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }

    public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
        if (this.blend) {
            GlStateManager.getBlendState(this.oldBlendState);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
        }

        this.resetStyles();
        this.textColor = textColor;
        str = this.trimStringNewline(str);
        this.renderSplitString(str, x, y, wrapWidth, false);

        if (this.blend) {
            GlStateManager.setBlendState(this.oldBlendState);
        }
    }

    private void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) {
        for (String s : this.listFormattedStringToWidth(str, wrapWidth)) {
            this.renderStringAligned(s, x, y, wrapWidth, this.textColor, addShadow);
            y += this.FONT_HEIGHT;
        }
    }

    public int splitStringWidth(String str, int maxLength) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(str, maxLength).size();
    }

    public void setUnicodeFlag(boolean unicodeFlagIn) {
        this.unicodeFlag = unicodeFlagIn;
    }

    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    public void setBidiFlag(boolean bidiFlagIn) {
        this.bidiFlag = bidiFlagIn;
    }

    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        return Arrays.asList(this.wrapFormattedStringToWidth(str, wrapWidth).split("\n"));
    }

    String wrapFormattedStringToWidth(String str, int wrapWidth) {
        if (str.length() <= 1) {
            return str;
        } else {
            int i = this.sizeStringToWidth(str, wrapWidth);

            if (str.length() <= i) {
                return str;
            } else {
                String s = str.substring(0, i);
                char c0 = str.charAt(i);
                boolean flag = c0 == 32 || c0 == 10;
                String s1 = getFormatFromString(s) + str.substring(i + (flag ? 1 : 0));
                return s + "\n" + this.wrapFormattedStringToWidth(s1, wrapWidth);
            }
        }
    }

    private int sizeStringToWidth(String str, int wrapWidth) {
        int i = str.length();
        float f = 0.0F;
        int j = 0;
        int k = -1;

        for (boolean flag = false; j < i; ++j) {
            char c0 = str.charAt(j);

            switch (c0) {
                case '\n':
                    --j;
                    break;

                case ' ':
                    k = j;
                    break;

                case '\u00a7':
                    if (j < i - 1) {
                        ++j;
                        char c1 = str.charAt(j);

                        if (c1 != 108 && c1 != 76) {
                            if (c1 == 114 || c1 == 82 || isFormatColor(c1)) {
                                flag = false;
                            }
                        } else {
                            flag = true;
                        }
                    }
                    break;

                default:
                    f += this.getCharWidth(c0);

                    if (flag) {
                        ++f;
                    }

                    break;
            }

            if (c0 == 10) {
                ++j;
                k = j;
                break;
            }

            if (Math.round(f) > wrapWidth) {
                break;
            }
        }

        return j != i && k != -1 && k < j ? k : j;
    }

    private static boolean isFormatColor(char colorChar) {
        return colorChar >= 48 && colorChar <= 57 || colorChar >= 97 && colorChar <= 102 || colorChar >= 65 && colorChar <= 70;
    }

    private static boolean isFormatSpecial(char formatChar) {
        return formatChar >= 107 && formatChar <= 111 || formatChar >= 75 && formatChar <= 79 || formatChar == 114 || formatChar == 82;
    }

    public static String getFormatFromString(String text) {
        String s = "";
        int i = -1;
        int j = text.length();

        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i < j - 1) {
                char c0 = text.charAt(i + 1);

                if (isFormatColor(c0)) {
                    s = "\u00a7" + c0;
                } else if (isFormatSpecial(c0)) {
                    s += "\u00a7" + c0;
                }
            }
        }

        return s;
    }

    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    public int getColorCode(char character) {
        int charIndex = "0123456789abcdef".indexOf(character);

        if (charIndex >= 0 && charIndex < this.colorCode.length) {
            int colorIndex = this.colorCode[charIndex];

            if (Config.isCustomColors()) {
                colorIndex = CustomColors.getTextColor(charIndex, colorIndex);
            }

            return colorIndex;
        } else {
            return 16777215;
        }
    }

    protected void bindTexture(ResourceLocation resource) {
        this.renderEngine.bindTexture(resource);
    }

    protected InputStream getResourceInputStream(ResourceLocation resource) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();
    }
}
