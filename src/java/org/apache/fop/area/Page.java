/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: Page.java,v 1.4 2004/02/27 17:41:26 jeremias Exp $ */

package org.apache.fop.area;

import java.io.Serializable;
import java.util.Map;

import org.apache.fop.fo.pagination.Region;

/**
 * The page.
 * This holds the contents of the page. Each region is added.
 * The unresolved references area added so that if the page is
 * serialized then it will handle the resolving properly after
 * being reloaded.
 * This is serializable so it can be saved to cache to save
 * memory if there are forward references.
 * The page is cloneable so the page master can make copies of
 * the top level page and regions.
 */
public class Page implements Serializable, Cloneable {
    // contains before, start, body, end and after regions
    private RegionViewport regionBefore = null;
    private RegionViewport regionStart = null;
    private RegionViewport regionBody = null;
    private RegionViewport regionEnd = null;
    private RegionViewport regionAfter = null;

    // temporary map of unresolved objects used when serializing the page
    private Map unresolved = null;

    /**
     * Set the region on this page.
     *
     * @param areaclass the area class of the region to set
     * @param port the region viewport to set
     */
    public void setRegionViewport(int areaclass, RegionViewport port) {
        if (areaclass == Region.BEFORE_CODE) {
            regionBefore = port;
        } else if (areaclass == Region.START_CODE) {
            regionStart = port;
        } else if (areaclass == Region.BODY_CODE) {
            regionBody = port;
        } else if (areaclass == Region.END_CODE) {
            regionEnd = port;
        } else if (areaclass == Region.AFTER_CODE) {
            regionAfter = port;
        }
    }

    /**
     * Get the region from this page.
     *
     * @param areaclass the region area class
     * @return the region viewport or null if none
     */
    public RegionViewport getRegionViewport(int areaclass) {
        if (areaclass == Region.BEFORE_CODE) {
            return regionBefore;
        } else if (areaclass == Region.START_CODE) {
            return regionStart;
        } else if (areaclass == Region.BODY_CODE) {
            return regionBody;
        } else if (areaclass == Region.END_CODE) {
            return regionEnd;
        } else if (areaclass == Region.AFTER_CODE) {
            return regionAfter;
        }
        return null;
    }

    /**
     * indicates whether any FOs have been added to the body region
     *
     * @return whether any FOs have been added to the body region
     */
    public boolean isEmpty() {
        if (regionBody == null) {
            return true;
        }
        else {
            BodyRegion body = (BodyRegion)regionBody.getRegion();
            return body.isEmpty();
        }
    }

    /**
     * Clone this page.
     * This returns a new page with a clone of all the regions.
     *
     * @return a new clone of this page
     */
    public Object clone() {
        Page p = new Page();
        if (regionBefore != null) {
            p.regionBefore = (RegionViewport)regionBefore.clone();
        }
        if (regionStart != null) {
            p.regionStart = (RegionViewport)regionStart.clone();
        }
        if (regionBody != null) {
            p.regionBody = (RegionViewport)regionBody.clone();
        }
        if (regionEnd != null) {
            p.regionEnd = (RegionViewport)regionEnd.clone();
        }
        if (regionAfter != null) {
            p.regionAfter = (RegionViewport)regionAfter.clone();
        }

        return p;
    }

    /**
     * Set the unresolved references on this page for serializing.
     *
     * @param unres the map of unresolved objects
     */
    public void setUnresolvedReferences(Map unres) {
        unresolved = unres;
    }

    /**
     * Get the map unresolved references from this page.
     * This should be called after deserializing to retrieve
     * the map of unresolved references that were serialized.
     *
     * @return the de-serialized map of unresolved objects
     */
    public Map getUnresolvedReferences() {
        return unresolved;
    }
}

