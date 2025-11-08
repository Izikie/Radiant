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
package net.radiant.lwjgl.util.glu;

import net.radiant.lwjgl.opengl.Util;

/**
 * GLU.java
 * <p>
 * <p>
 * Created 23-dec-2003
 *
 * @author Erik Duijs
 */
public class GLU {
    /* Errors: (return value 0 = no error) */
    public static final int GLU_INVALID_ENUM = 100900;
    public static final int GLU_INVALID_VALUE = 100901;
    public static final int GLU_OUT_OF_MEMORY = 100902;
    public static final int GLU_INCOMPATIBLE_GL_VERSION = 100903;
    /* StringName */
    public static final int GLU_VERSION = 100800;
    public static final int GLU_EXTENSIONS = 100801;
    /* Boolean */
    public static final boolean GLU_TRUE = true;
    public static final boolean GLU_FALSE = false;
    /****           Quadric constants               ****/

    /* QuadricNormal */
    public static final int GLU_SMOOTH = 100000;
    public static final int GLU_FLAT = 100001;
    public static final int GLU_NONE = 100002;
    /* QuadricDrawStyle */
    public static final int GLU_POINT = 100010;
    public static final int GLU_LINE = 100011;
    public static final int GLU_FILL = 100012;
    public static final int GLU_SILHOUETTE = 100013;
    /* QuadricOrientation */
    public static final int GLU_OUTSIDE = 100020;
    public static final int GLU_INSIDE = 100021;
    /****           Tesselation constants           ****/

    public static final double GLU_TESS_MAX_COORD = 1.0e150;

    /* Callback types: */
    /*      ERROR               = 100103 */
    public static final double TESS_MAX_COORD = 1.0e150;
    /* TessProperty */
    public static final int GLU_TESS_WINDING_RULE = 100140;
    public static final int GLU_TESS_BOUNDARY_ONLY = 100141;
    public static final int GLU_TESS_TOLERANCE = 100142;
    /* TessWinding */
    public static final int GLU_TESS_WINDING_ODD = 100130;
    public static final int GLU_TESS_WINDING_NONZERO = 100131;
    public static final int GLU_TESS_WINDING_POSITIVE = 100132;
    public static final int GLU_TESS_WINDING_NEGATIVE = 100133;
    public static final int GLU_TESS_WINDING_ABS_GEQ_TWO = 100134;
    /* TessCallback */
    public static final int GLU_TESS_BEGIN = 100100;  /* void (CALLBACK*)(GLenum    type)  */
    public static final int GLU_TESS_VERTEX = 100101;  /* void (CALLBACK*)(void      *data) */
    public static final int GLU_TESS_END = 100102;  /* void (CALLBACK*)(void)            */
    public static final int GLU_TESS_ERROR = 100103;  /* void (CALLBACK*)(GLenum    errno) */
    public static final int GLU_TESS_EDGE_FLAG = 100104;  /* void (CALLBACK*)(GLboolean boundaryEdge)  */
    public static final int GLU_TESS_COMBINE = 100105;  /* void (CALLBACK*)(GLdouble  coords[3],
	                                                            void      *data[4],
	                                                            GLfloat   weight[4],
	                                                            void      **dataOut)     */
    public static final int GLU_TESS_BEGIN_DATA = 100106;  /* void (CALLBACK*)(GLenum    type,
	                                                            void      *polygon_data) */
    public static final int GLU_TESS_VERTEX_DATA = 100107;  /* void (CALLBACK*)(void      *data,
	                                                            void      *polygon_data) */
    public static final int GLU_TESS_END_DATA = 100108;  /* void (CALLBACK*)(void      *polygon_data) */
    public static final int GLU_TESS_ERROR_DATA = 100109;  /* void (CALLBACK*)(GLenum    errno,
	                                                            void      *polygon_data) */
    public static final int GLU_TESS_EDGE_FLAG_DATA = 100110;  /* void (CALLBACK*)(GLboolean boundaryEdge,
	                                                            void      *polygon_data) */
    public static final int GLU_TESS_COMBINE_DATA = 100111;  /* void (CALLBACK*)(GLdouble  coords[3],
	                                                            void      *data[4],
	                                                            GLfloat   weight[4],
	                                                            void      **dataOut,
	                                                            void      *polygon_data) */
    /* TessError */
    public static final int GLU_TESS_ERROR1 = 100151;
    public static final int GLU_TESS_ERROR2 = 100152;
    public static final int GLU_TESS_ERROR3 = 100153;
    public static final int GLU_TESS_ERROR4 = 100154;
    public static final int GLU_TESS_ERROR5 = 100155;
    public static final int GLU_TESS_ERROR6 = 100156;
    public static final int GLU_TESS_ERROR7 = 100157;
    public static final int GLU_TESS_ERROR8 = 100158;
    public static final int GLU_TESS_MISSING_BEGIN_POLYGON = GLU_TESS_ERROR1;
    public static final int GLU_TESS_MISSING_BEGIN_CONTOUR = GLU_TESS_ERROR2;
    public static final int GLU_TESS_MISSING_END_POLYGON = GLU_TESS_ERROR3;
    public static final int GLU_TESS_MISSING_END_CONTOUR = GLU_TESS_ERROR4;
    public static final int GLU_TESS_COORD_TOO_LARGE = GLU_TESS_ERROR5;
    public static final int GLU_TESS_NEED_COMBINE_CALLBACK = GLU_TESS_ERROR6;
    static final float PI = (float) Math.PI;

    public static String gluErrorString(int error_code) {
        return switch (error_code) {
            case GLU_INVALID_ENUM -> "Invalid enum (glu)";
            case GLU_INVALID_VALUE -> "Invalid value (glu)";
            case GLU_OUT_OF_MEMORY -> "Out of memory (glu)";
            default -> Util.translateGLErrorString(error_code);
        };
    }
}
