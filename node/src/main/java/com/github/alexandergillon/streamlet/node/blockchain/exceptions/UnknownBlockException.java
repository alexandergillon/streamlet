/*
 * Copyright (C) 2023 Alexander Gillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.alexandergillon.streamlet.node.blockchain.exceptions;

/**
 * Exception thrown when a block cannot be found in the blockchain. Catchers should initiate a catch-up process by
 * querying other nodes in the network.
 */
public class UnknownBlockException extends Exception {

    public UnknownBlockException() {
        super();
    }

    public UnknownBlockException(String message) {
        super(message);
    }

    public UnknownBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownBlockException(Throwable cause) {
        super(cause);
    }

}
