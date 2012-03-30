/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.ps.fonts;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.fop.fonts.truetype.TTFGlyphOutputStream;
import org.apache.fop.fonts.truetype.TTFTableOutputStream;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Tests PSTTFOuputStream
 */
public class PSTTFOutputStreamTest extends TestCase {
    private PSGenerator gen;
    private PSTTFOutputStream out;

    /**
     * Assigns an OutputStream to the PSGenerator.
     */
    public void setUp() {
        gen = mock(PSGenerator.class);
        out = new PSTTFOutputStream(gen);
    }

    /**
     * Test startFontStream() - Just tests that the font is properly initiated in the PostScript
     * document (in this case with "/sfnts[")
     * @throws IOException write exception.
     */
    public void testStartFontStream() throws IOException {
        out.startFontStream();
        verify(gen).write("/sfnts[");
    }

    /**
     * Test getTableOutputStream() - we need to test that the inheritance model is properly obeyed.
     */
    public void testGetTableOutputStream() {
        TTFTableOutputStream tableOut = out.getTableOutputStream();
        assertTrue(tableOut instanceof PSTTFTableOutputStream);
    }

    /**
     * Test getGlyphOutputStream() - we need to test that the inheritance model is properly obeyed.
     */
    public void testGetGlyphOutputStream() {
        TTFGlyphOutputStream glyphOut = out.getGlyphOutputStream();
        assertTrue(glyphOut instanceof PSTTFGlyphOutputStream);
    }

    /**
     * Test endFontStream()
     * @exception IOException write error.
     */
    public void testEndFontStream() throws IOException {
        out.endFontStream();
        verify(gen).writeln("] def");
    }
}