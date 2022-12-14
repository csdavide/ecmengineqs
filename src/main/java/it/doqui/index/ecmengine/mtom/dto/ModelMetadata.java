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

package it.doqui.index.ecmengine.mtom.dto;

import it.doqui.index.ecmengine.util.StringUtil;

/**
 * DTO che rappresenta la definizione di un modello dei dati.
 * 
 * @author DoQui
 */
public class ModelMetadata extends MtomEngineDto {
    private static final long serialVersionUID = 1478798656943319686L;
    
    private String prefixedName;
    private String description;
    private TypeMetadata[] types;
    private AspectMetadata[] aspects;

    /**
     * Restituisce il prefixed name del modello.
     */
    public String getPrefixedName() {
	return prefixedName;
    }

    /**
     * Imposta il prefixed name del modello.
     */
    public void setPrefixedName(String prefixedName) {
	this.prefixedName = prefixedName;
    }

    /**
     * Restituisce la definizione dei tipi del modello.
     */
    public TypeMetadata[] getTypes() {
	return types;
    }

    /**
     * Imposta la definizione dei tipi del modello.
     */
    public void setTypes(TypeMetadata[] types) {
	this.types = types;
    }

    /**
     * Restituisce la descrizione del modello.
     */
    public String getDescription() {
	return description;
    }

    /**
     * Imposta la descrizione del modello.
     */
    public void setDescription(String description) {
	this.description = description;
    }

    /**
     * Restituisce la definizione degli aspetti del modello.
     */
    public AspectMetadata[] getAspects() {
	return aspects;
    }

    /**
     * Imposta la definizione degli aspetti del modello.
     */
    public void setAspects(AspectMetadata[] aspects) {
	this.aspects = aspects;
    }

    public String toString() {
	return "ModelMetadata [prefixedName=" + prefixedName + ", description=" + description + ", types-length="
		+ StringUtil.toLength(types) + ", aspects-length=" + StringUtil.toLength(aspects) + "]";
    }
}
