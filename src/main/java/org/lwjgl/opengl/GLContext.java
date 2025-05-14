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

import org.lwjgl.LWJGLException;

import javax.annotation.Nullable;

/**
 * Created by gudenau on 5/31/2017.
 * <p>
 * LWJGL3
 */
public class GLContext {
    private static final ThreadLocal<org.lwjgl.opengl.ContextCapabilities> current_capabilities = new ThreadLocal<>();

    public static org.lwjgl.opengl.ContextCapabilities getCapabilities() {
        org.lwjgl.opengl.ContextCapabilities caps = getCapabilitiesImpl();
        if (caps == null) {
            //throw new RuntimeException("No OpenGL context found in the current thread.");
            try {
                org.lwjgl.opengl.ContextCapabilities created = new org.lwjgl.opengl.ContextCapabilities(false);
                setCapabilities(created);
                return created;
            } catch (LWJGLException e) {
                //e.printStackTrace();
                throw new RuntimeException("No OpenGL context found in the current thread and could not create!", e);
            }
        }

        return caps;
    }

    static void setCapabilities(org.lwjgl.opengl.ContextCapabilities capabilities) {
        current_capabilities.set(capabilities);
    }

    private static @Nullable org.lwjgl.opengl.ContextCapabilities getCapabilitiesImpl() {
        return getThreadLocalCapabilities();
    }

    private static @Nullable ContextCapabilities getThreadLocalCapabilities() {
        return current_capabilities.get();
    }
}
