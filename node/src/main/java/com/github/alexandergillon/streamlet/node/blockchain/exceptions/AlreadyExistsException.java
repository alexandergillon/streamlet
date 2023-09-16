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

import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;
import lombok.Getter;

/** Exception thrown when a block that already exists in the blockchain is inserted into the tree. */
public class AlreadyExistsException extends Exception {

    /** The already existing node in the block tree, which prevented the block from being inserted. */
    @Getter
    private final BlockTree existingNode;

    /**
     * @param existingNode The already existing node in the block tree
     * @param message Exception message.
     */
    public AlreadyExistsException(BlockTree existingNode, String message) {
        super(message);
        this.existingNode = existingNode;
    }
}
