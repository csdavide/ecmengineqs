/* Index ECM Engine - A system for managing the capture (when created 
 * or received), classification (cataloguing), storage, retrieval, 
 * revision, sharing, reuse and disposition of documents.
 *
 * Copyright (C) 2008 Regione Piemonte
 * Copyright (C) 2008 Provincia di Torino
 * Copyright (C) 2008 Comune di Torino
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package it.doqui.index.ecmengine.mtom.exception;

/**
 * Eccezione lanciata in caso di errore nella cancellazione di un gruppo.
 * 
 * @author DoQui
 */
public class GroupDeleteException extends MtomException {
    private static final long serialVersionUID = -3161654415007448354L;

    /**
     * Costruttore che prende in input il nome di un gruppo.
     */
    public GroupDeleteException(String groupName) {
	super("Impossibile eliminare il gruppo: " + groupName);
    }

    /**
     * Costruttore che prende in input il nome di un gruppo e la causa
     * dell'eccezione.
     */
    public GroupDeleteException(String groupName, Throwable cause) {
	super("Impossibile eliminare il gruppo: " + groupName);
	initCause(cause);
    }
}
