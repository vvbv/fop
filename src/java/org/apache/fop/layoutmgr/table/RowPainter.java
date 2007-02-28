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

package org.apache.fop.layoutmgr.table;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.fo.flow.TableRow;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.SpaceResolver;
import org.apache.fop.layoutmgr.table.TableContentLayoutManager.GridUnitPart;
import org.apache.fop.layoutmgr.table.TableContentLayoutManager.TableContentPosition;

class RowPainter {
    private static Log log = LogFactory.getLog(RowPainter.class);
    private TableRow rowFO = null;
    private int colCount;
    private int yoffset = 0;
    private int accumulatedBPD = 0;
    private EffRow lastRow = null;
    private LayoutContext layoutContext;
    private int lastRowHeight = 0;
    private int[] firstRow = new int[3];
    private Map[] rowOffsets = new Map[] {new java.util.HashMap(),
            new java.util.HashMap(), new java.util.HashMap()};

    //These three variables are our buffer to recombine the individual steps into cells
    private PrimaryGridUnit[] gridUnits;
    private int[] start;
    private int[] end;
    private int[] partLength;
    private TableContentLayoutManager tclm;

    public RowPainter(TableContentLayoutManager tclm, LayoutContext layoutContext) {
        this.tclm = tclm;
        this.layoutContext = layoutContext;
        this.colCount = tclm.getColumns().getColumnCount();
        this.gridUnits = new PrimaryGridUnit[colCount];
        this.start = new int[colCount];
        this.end = new int[colCount];
        this.partLength = new int[colCount];
        Arrays.fill(firstRow, -1);
        Arrays.fill(end, -1);
    }

    public int getAccumulatedBPD() {
        return this.accumulatedBPD;
    }

    public void notifyEndOfSequence() {
        this.accumulatedBPD += lastRowHeight; //for last row
    }

    public void notifyNestedPenaltyArea(int length) {
        this.lastRowHeight += length;
    }

    public void handleTableContentPosition(TableContentPosition tcpos) {
        if (lastRow != tcpos.row && lastRow != null) {
            addAreasAndFlushRow(false);
            yoffset += lastRowHeight;
            this.accumulatedBPD += lastRowHeight;
        }
        if (log.isDebugEnabled()) {
            log.debug("===handleTableContentPosition(" + tcpos);
        }
        rowFO = tcpos.row.getTableRow();
        lastRow = tcpos.row;
        Iterator partIter = tcpos.gridUnitParts.iterator();
        //Iterate over all grid units in the current step
        while (partIter.hasNext()) {
            GridUnitPart gup = (GridUnitPart)partIter.next();
            if (log.isDebugEnabled()) {
                log.debug(">" + gup);
            }
            int colIndex = gup.pgu.getStartCol();
            if (gridUnits[colIndex] != gup.pgu) {
                if (gridUnits[colIndex] != null) {
                    log.warn("Replacing GU in slot " + colIndex
                            + ". Some content may not be painted.");
                }
                gridUnits[colIndex] = gup.pgu;
                start[colIndex] = gup.start;
                end[colIndex] = gup.end;
            } else {
                if (gup.end < end[colIndex]) {
                    throw new IllegalStateException("Internal Error: stepper problem");
                }
                end[colIndex] = gup.end;
            }
        }
    }

    public int addAreasAndFlushRow(boolean forcedFlush) {
        int actualRowHeight = 0;
        int readyCount = 0;

        int bt = lastRow.getBodyType();
        if (log.isDebugEnabled()) {
            log.debug("Remembering yoffset for row " + lastRow.getIndex() + ": " + yoffset);
        }
        rowOffsets[bt].put(new Integer(lastRow.getIndex()), new Integer(yoffset));

        for (int i = 0; i < gridUnits.length; i++) {
            if ((gridUnits[i] != null)
                    && (forcedFlush || (end[i] == gridUnits[i].getElements().size() - 1))) {
                if (log.isTraceEnabled()) {
                    log.trace("getting len for " + i + " "
                            + start[i] + "-" + end[i]);
                }
                readyCount++;
                int len = ElementListUtils.calcContentLength(
                        gridUnits[i].getElements(), start[i], end[i]);
                partLength[i] = len;
                if (log.isTraceEnabled()) {
                    log.trace("len of part: " + len);
                }

                if (start[i] == 0) {
                    LengthRangeProperty bpd = gridUnits[i].getCell()
                            .getBlockProgressionDimension();
                    if (!bpd.getMinimum(tclm.getTableLM()).isAuto()) {
                        int min = bpd.getMinimum(tclm.getTableLM())
                                    .getLength().getValue(tclm.getTableLM());
                        if (min > 0) {
                            len = Math.max(len, bpd.getMinimum(tclm.getTableLM())
                                    .getLength().getValue(tclm.getTableLM()));
                        }
                    }
                    if (!bpd.getOptimum(tclm.getTableLM()).isAuto()) {
                        int opt = bpd.getOptimum(tclm.getTableLM())
                                    .getLength().getValue(tclm.getTableLM());
                        if (opt > 0) {
                            len = Math.max(len, opt);
                        }
                    }
                    if (gridUnits[i].getRow() != null) {
                        bpd = gridUnits[i].getRow().getBlockProgressionDimension();
                        if (!bpd.getMinimum(tclm.getTableLM()).isAuto()) {
                            int min = bpd.getMinimum(tclm.getTableLM()).getLength()
                                        .getValue(tclm.getTableLM());
                            if (min > 0) {
                                len = Math.max(len, min);
                            }
                        }
                    }
                }

                // Add the padding if any
                len += gridUnits[i].getBorders()
                                .getPaddingBefore(false, gridUnits[i].getCellLM());
                len += gridUnits[i].getBorders()
                                .getPaddingAfter(false, gridUnits[i].getCellLM());

                //Now add the borders to the contentLength
                if (tclm.isSeparateBorderModel()) {
                    len += gridUnits[i].getBorders().getBorderBeforeWidth(false);
                    len += gridUnits[i].getBorders().getBorderAfterWidth(false);
                }
                int startRow = Math.max(gridUnits[i].getStartRow(), firstRow[bt]);
                Integer storedOffset = (Integer)rowOffsets[bt].get(new Integer(startRow));
                int effYOffset;
                if (storedOffset != null) {
                    effYOffset = storedOffset.intValue();
                } else {
                    effYOffset = yoffset;
                }
                len -= yoffset - effYOffset;
                actualRowHeight = Math.max(actualRowHeight, len);
            }
        }
        if (readyCount == 0) {
            return 0;
        }
        actualRowHeight += 2 * tclm.getTableLM().getHalfBorderSeparationBPD();
        lastRowHeight = actualRowHeight;

        //Add areas for row
        tclm.addRowBackgroundArea(rowFO, actualRowHeight, layoutContext.getRefIPD(), yoffset);
        for (int i = 0; i < gridUnits.length; i++) {
            GridUnit currentGU = lastRow.safelyGetGridUnit(i);
            if ((gridUnits[i] != null)
                    && (forcedFlush || ((end[i] == gridUnits[i].getElements().size() - 1))
                            && (currentGU == null || currentGU.isLastGridUnitRowSpan()))
                || (gridUnits[i] == null && currentGU != null)) {
                //the last line in the "if" above is to avoid a premature end of an
                //row-spanned cell because no GridUnitParts are generated after a cell is
                //finished with its content. currentGU can be null if there's no grid unit
                //at this place in the current row (empty cell and no borders to process)
                if (log.isDebugEnabled()) {
                    log.debug((forcedFlush ? "FORCED " : "") + "flushing..." + i + " "
                            + start[i] + "-" + end[i]);
                }
                PrimaryGridUnit gu = gridUnits[i];
                if (gu == null
                        && !currentGU.isEmpty()
                        && currentGU.getColSpanIndex() == 0
                        && currentGU.isLastGridUnitColSpan()
                        && (forcedFlush || currentGU.isLastGridUnitRowSpan())) {
                    gu = currentGU.getPrimary();
                }
                if (gu != null) {
                    addAreasForCell(gu, start[i], end[i],
                            lastRow,
                            partLength[i], actualRowHeight);
                    gridUnits[i] = null;
                    start[i] = 0;
                    end[i] = -1;
                    partLength[i] = 0;
                }
            }
        }
        return actualRowHeight;
    }

    private void addAreasForCell(PrimaryGridUnit pgu, int startPos, int endPos,
            EffRow row, int contentHeight, int rowHeight) {
        int bt = row.getBodyType();
        if (firstRow[bt] < 0) {
            firstRow[bt] = row.getIndex();
        }
        //Determine the first row in this sequence
        int startRow = Math.max(pgu.getStartRow(), firstRow[bt]);
        //Determine y offset for the cell
        Integer offset = (Integer)rowOffsets[bt].get(new Integer(startRow));
        while (offset == null) {
            startRow--;
            offset = (Integer)rowOffsets[bt].get(new Integer(startRow));
        }
        int effYOffset = offset.intValue();
        int effCellHeight = rowHeight;
        effCellHeight += yoffset - effYOffset;
        if (log.isDebugEnabled()) {
            log.debug("Creating area for cell:");
            log.debug("  current row: " + row.getIndex());
            log.debug("  start row: " + pgu.getStartRow() + " " + yoffset + " " + effYOffset);
            log.debug("  contentHeight: " + contentHeight + " rowHeight=" + rowHeight
                    + " effCellHeight=" + effCellHeight);
        }
        TableCellLayoutManager cellLM = pgu.getCellLM();
        cellLM.setXOffset(tclm.getXOffsetOfGridUnit(pgu));
        cellLM.setYOffset(effYOffset);
        cellLM.setContentHeight(contentHeight);
        cellLM.setRowHeight(effCellHeight);
        //cellLM.setRowHeight(row.getHeight().opt);
        int prevBreak = ElementListUtils.determinePreviousBreak(pgu.getElements(), startPos);
        if (endPos >= 0) {
            SpaceResolver.performConditionalsNotification(pgu.getElements(),
                    startPos, endPos, prevBreak);
        }
        cellLM.addAreas(new KnuthPossPosIter(pgu.getElements(),
                startPos, endPos + 1), layoutContext);
    }
}
