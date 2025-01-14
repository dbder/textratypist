/*
 * Copyright (c) 2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.textra;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.textra.utils.BlockUtils;

public class GridTest extends ApplicationAdapter {
    Font font;
    Font[] fonts;
    SpriteBatch batch;
    int[][] backgrounds;
    char[][] lines;
    Layout layout;
    TypingLabel marquee;
    long startTime;
    RandomXS128 random;

    static final int PIXEL_WIDTH = 800, PIXEL_HEIGHT = 640;

    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Font test");
        config.setWindowedMode(PIXEL_WIDTH, PIXEL_HEIGHT);
        config.disableAudio(true);
        config.useVsync(true);
        new Lwjgl3Application(new GridTest(), config);
    }
    /**
     * Interpolates from the packed int color start towards end by change. Both start and end should be packed RGBA8888
     * ints, and change can be between 0f (keep start) and 1f (only use end). This is a good way to reduce allocations
     * of temporary Colors.
     *
     * @param s      the starting color as a packed int
     * @param e      the end/target color as a packed int
     * @param change how much to go from start toward end, as a float between 0 and 1; higher means closer to end
     * @return a packed RGBA8888 int that represents a color between start and end
     */
    public static int lerpColors(final int s, final int e, final float change) {
        final int
                sR = (s & 0xFF), sA = (s >>> 8) & 0xFF, sB = (s >>> 16) & 0xFF, sAlpha = s >>> 24 & 0xFF,
                eR = (e & 0xFF), eA = (e >>> 8) & 0xFF, eB = (e >>> 16) & 0xFF, eAlpha = e >>> 24 & 0xFF;
        return (((int) (sR + change * (eR - sR)) & 0xFF)
                | (((int) (sA + change * (eA - sA)) & 0xFF) << 8)
                | (((int) (sB + change * (eB - sB)) & 0xFF) << 16)
                | (((int) (sAlpha + change * (eAlpha - sAlpha)) & 0xFF) << 24));
    }

    @Override
    public void create() {
        random = new RandomXS128(123);
        lines = new char[40][10];
        int c = -1;
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 10; y++) {
                do {
                    c = (c + 1) % BlockUtils.BOX_DRAWING.length;
                } while (BlockUtils.BOX_DRAWING[c].length == 0);
                lines[x][y] = (char) (0x2500 + c);
            }
        }

        batch = new SpriteBatch();

        fonts = KnownFonts.getAll();
        for(Font f : fonts)
            f.scaleTo(25f * f.cellWidth / f.cellHeight, 25).fitCell(25, 25, true);
        font = fonts[0];

//        font = KnownFonts.getInconsolata();//.scaleTo(16, 32);
//        font = KnownFonts.getCascadiaMono().scaleTo(12, 24);
//        font = KnownFonts.getIosevka();
//        font = KnownFonts.getIosevkaSlab().scale(0.75f, 0.75f);
//        font = KnownFonts.getIosevkaSlabMSDF().scaleTo(20, 20);
//        font = KnownFonts.getDejaVuSansMono().scale(0.75f, 0.75f);
//        font = KnownFonts.getCozette().scale(2, 2);
//        font = KnownFonts.getOpenSans().scale(0.75f, 0.75f);
//        font = KnownFonts.getAStarry();
//        font = KnownFonts.getGentium().scaleTo(48, 48);
//        font = KnownFonts.getLibertinusSerif();
//        font = KnownFonts.getKingthingsFoundation().scaleTo(45, 60);
//        font = KnownFonts.getOxanium();
//        font = KnownFonts.getYanoneKaffeesatz().scaleTo(45, 60);
//        font = KnownFonts.getCanada().scaleTo(40, 58);
//        font = KnownFonts.getRobotoCondensed();
//        font = KnownFonts.getIBM8x16();
//        font = KnownFonts.getOpenSans();

//        font.fitCell(font.cellWidth, font.cellHeight, true);
//        font.fitCell(font.cellHeight, font.cellHeight, true);
        font.scaleTo(font.cellWidth * 25f / font.cellHeight, 25f).fitCell(25f, 25f, true);

        layout = new Layout(font).setTargetWidth(Gdx.graphics.getWidth());
        backgrounds = new int[(int) Math.ceil(PIXEL_WIDTH / font.cellWidth)][(int) Math.ceil(PIXEL_HEIGHT / font.cellHeight)];
        int sw = 0x669E83FF, se = 0x2A8528FF, nw = 0xF0DDA0FF, ne = 0x7A4A31FF;
        backgrounds[0][0] = sw;
        backgrounds[0][backgrounds[0].length - 1] = nw;
        backgrounds[backgrounds.length - 1][0] = se;
        backgrounds[backgrounds.length - 1][backgrounds[0].length - 1] = ne;
        for (int x = 1; x < backgrounds.length - 1; x++) {
            backgrounds[x][0] = lerpColors(sw, se, x / (float) backgrounds.length);
            backgrounds[x][backgrounds[0].length - 1] = lerpColors(nw, ne, x / (float) backgrounds.length);
        }
        for (int x = 0; x < backgrounds.length; x++) {
            int s = backgrounds[x][0], e = backgrounds[x][backgrounds[0].length - 1];
            for (int y = 1; y < backgrounds[0].length - 1; y++) {
                backgrounds[x][y] = lerpColors(s, e, y / (float) backgrounds[0].length);
            }
        }

//        font.markup("[#00FF00FF]Hello, [~]World[~]Universe[.]$[=]$[^]$[^]!", glyphs[0] = new LongList());
//        font.markup("The [dark richer red]MAW[] of the [/][|lighter blue mint]wendigo[] [*]appears[*]!", glyphs[1] = new LongList());
//        font.markup("The [_][dark dull blue purple]BLADE[] of [*][/][|dark richest yellow]KINGS[] strikes!", glyphs[2] = new LongList());
//        font.markup("[;]Each cap[], [,]All lower[], [!]Caps lock[], [?]Unknown[]?", glyphs[3] = new LongList());

//        font.markup("[#007711FF]Hello, [~]World[~]Universe[.]$[=]$[^]$[^]!", layout);
//
//        font.markup("\n[*]Водяной[] — в славянской мифологии дух, обитающий в воде, хозяин вод[^][BLUE][[2][]."
//                + "\nВоплощение стихии воды как отрицательного и опасного начала[^][BLUE][[3][[citation needed][].", layout);
//
        font.markup("Test [*]TEST [/]Test [*]TEST[][.]test [=]Test [^]TEST [][_]Test [~]test[_] Test[]"
                        + "\nThe [#800000]MAW[] of the [/][#66DDFF]wendigo[/] (wendigo)[] [*]appears[*]!"
                        + "\nThe [_][#666666]BLADE[] of [*][/][#FFFF44]DYNAST-KINGS[] strikes!"
                        + "\n[_][;]Each cap, [,]All lower, [!]Caps lock[], [?]Unknown[]?"
                        + "\n[#BBAA44]φ[] = (1 + 5[^]0.5[^]) * 0.5"
                        + "\n[#FF8822]¿Qué son estos? ¡Arribate, mijo![]"
                        + "\nPchnąć[] w tę łódź [#775522]jeża[] lub ośm skrzyń [#CC00CC]fig[]."
                , layout);

        marquee = new TypingLabel("{BLINK}{ROTATE=90}EAT AT JOE'S", font);
        marquee.wrap = false;
        marquee.parseTokens();
        marquee.setWidth(Gdx.graphics.getWidth());
        marquee.setPosition(32, 400);
        marquee.skipToTheEnd();
        marquee.setRotation(-90f);
//        font.markup("\"You are ever more the [/]fool[/] than the pitiable cutpurse who [*]dares waylay[*] my castle road!\" the [dark rich gold]King[] admonished."
//                +" \"Forsooth! Had [_]I[_] my right mind, I would have [dark red]both of [_]your heads[] by morning. But alas, I am stricken with" +
//                " unreasonable mercy for your [~]wretched[~] souls. To [darker grey][*]the Trappists[] ye shall go; I am in need of" +
//                " a [darkest bronze]stout brew[].\"", layout);

//        font.markup("\"[/][~]HOSTILE ACTION DETECTED[].\" The computerized voice was barely audible over the klaxons blaring throughout [darker rich purple][_]Starship Andromalius[]."
//                +" \"[!]Would somebody shut that thing off[!]? We're quite aware by now!\" [orange]Captain Luiz Tigre[] shouted at no one in particular, while frantically flipping the remaining" +
//                " switches on the capacitor controls. \"Sir, we need to get the [silver]teleprojector[] online. Send a party aboard, say they're negotiators.\" [light sky]First Admiral Zototh[] said with urgency." +
//                " \"[*]Negotiators[*]? Are you serious?\" \"I said to [/]say[/] they're negotiators... just with really big guns.\"", layout);

        startTime = TimeUtils.millis();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.4f, 0.5f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float x = 0, y = layout.getHeight();
        long since = TimeUtils.timeSinceMillis(startTime);
        font = fonts[(int) (since >>> 10 & 0x7FFFFFFF) % fonts.length];
        marquee.act(Gdx.graphics.getDeltaTime());
        batch.begin();
        font.enableShader(batch);

        font.drawBlocks(batch, backgrounds, 0f, 0f);
        for (int xx = 0; xx < lines.length; xx++) {
            for (int yy = 0; yy < lines[0].length; yy++) {
                font.drawGlyph(batch, 0xFFFFFFFE00000000L | lines[xx][yy], font.cellWidth * xx, y + font.cellHeight * (1 + yy));
            }
        }
//        long color = (long) DescriptiveColor.lerpColors(
//                DescriptiveColor.lerpColors(
//                        (int)((System.currentTimeMillis() >>> 10) * 0x9E3779B0 | 0xFE),
//                        (int)(((System.currentTimeMillis() >>> 10) + 1L) * 0x9E3779B0 | 0xFE),
//                        (System.currentTimeMillis() & 0x3FFL) * 0x1p-10f
//                ), 0x000000FF, 0.375f) << 32;
//        for (int i = 0, n = glyphs[0].size(); i < n; i++) {
//            glyphs[0].set(i, glyphs[0].get(i) & 0xFFFFFFFFL | color);
//        }
        font.drawGlyphs(batch, layout,
//0f, y, Align.left
                PIXEL_WIDTH * 0.5f, y, Align.center
        );
        font.drawGlyph(batch, Font.markupGlyph(font.solidBlock, "[RED]", ColorLookup.INSTANCE), 0, Gdx.graphics.getHeight() - font.cellHeight);
        marquee.draw(batch, 1f);
        batch.end();
        Gdx.graphics.setTitle(font.name + " at " + Gdx.graphics.getFramesPerSecond() + " FPS");
    }

    @Override
    public void resize(int width, int height) {
        font.resizeDistanceField(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
}
