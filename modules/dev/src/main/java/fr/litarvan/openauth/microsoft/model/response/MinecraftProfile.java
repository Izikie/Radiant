/*
 * Copyright 2015-2021 Adrien 'Litarvan' Navratil
 *
 * This file is part of OpenAuth.

 * OpenAuth is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAuth.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.litarvan.openauth.microsoft.model.response;

/**
 * Minecraft player profile
 *
 * <p>
 * Represents a Minecraft player profile data. UUID is {@link #id} and username is {@link #name}.
 * </p>
 *
 * @author Litarvan
 * @version 1.1.0
 */
public record MinecraftProfile(String id, String name, MinecraftSkin[] skins) {

    /**
     * @return The player Minecraft UUID
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * @return The player Minecraft username
     */
    @Override
    public String name() {
        return name;
    }

    public record MinecraftSkin(String id, String state, String url, String variant, String alias) {
    }
}
