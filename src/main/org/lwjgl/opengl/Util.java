/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.opengl;

import static org.lwjgl.opengl.ARBImaging.GL_TABLE_TOO_LARGE;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;

/**
 * Simple utility class.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision$
 */

public final class Util {

    /**
     * Throws OpenGLException if glGetError() returns anything else than GL_NO_ERROR
     */
    public static void checkGLError() throws org.lwjgl.opengl.OpenGLException {
        if (ContextCapabilities.DEBUG) // Do not call GetError inside a Begin/End pair.
            return;
        int err = glGetError();
        if (err != GL_NO_ERROR) {
            throw new OpenGLException(err);
        }
    }

    /**
     * Translate a GL error code to a String describing the error
     */
    public static String translateGLErrorString(int error_code) {
        return switch (error_code) {
            case GL_NO_ERROR -> "No error";
            case GL_INVALID_ENUM -> "Invalid enum";
            case GL_INVALID_VALUE -> "Invalid value";
            case GL_INVALID_OPERATION -> "Invalid operation";
            case GL_STACK_OVERFLOW -> "Stack overflow";
            case GL_STACK_UNDERFLOW -> "Stack underflow";
            case GL_OUT_OF_MEMORY -> "Out of memory";
            case GL_TABLE_TOO_LARGE -> "Table too large";
            case GL_INVALID_FRAMEBUFFER_OPERATION -> "Invalid framebuffer operation";
            default -> null;
        };
    }
}