/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.DateTimeUtil;
import net.maxgigapop.mrs.common.ModelUtil;
import org.json.simple.JSONObject;
import java.lang.Math;

/**
 *
 * @author xyang
 */
public class BandwidthCalendar {
    public static final long infinite = Long.MAX_VALUE; 
    public static class BandwidthCalendarException extends Exception {
        public BandwidthCalendarException(String message) {
            super(message);
        }        
    }

    public static class BandwidthSchedule {
        long startTime = 0L;
        long endTime = infinite;
        long bandwidth = 0;
        
        public BandwidthSchedule() {
            
        
        }
        public BandwidthSchedule(long start, long end, long bw) {
            startTime = start;
            endTime = end;
            bandwidth = bw;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(long bandwidth) {
            this.bandwidth = bandwidth;
        }
        
        public boolean hasTime(long time) {
            return (startTime <= time && time < endTime);
        }
        
        public void addBandwidth(long bw) {
            bandwidth += bw;
        }
        
        public boolean reduceBandwidth(long bw) {
            bandwidth -= bw;
            if (bandwidth < 0) {
                bandwidth = 0;
                return false;
            }
            return true;
        }

        public BandwidthSchedule clone() {
            return new BandwidthSchedule(this.startTime, this.endTime, this.bandwidth);
        }
        
        public String toString() {
            return String.format("%d: (%d, %d)", bandwidth, startTime, endTime);
        }
    }
    
    
    long capacity = 0;
    List<BandwidthSchedule> schedules = new ArrayList();
    
    private BandwidthCalendar() {
    }

    public BandwidthCalendar(long capacity) {
        this.capacity = capacity;
    }

    public BandwidthCalendar(long capacity, long start, long end, long bw) {
        this.capacity = capacity;
        schedules.add(new BandwidthSchedule(start, end, bw));
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public List<BandwidthSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<BandwidthSchedule> schedules) {
        this.schedules = schedules;
    }
    
    public BandwidthSchedule lookupScheduleByTime(long time) {
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.hasTime(time)) {
                return schedule;
            }
        }
        return null;
    }
    
    public List<BandwidthSchedule> lookupSchedulesBetween (long start, long end) {
        List<BandwidthSchedule> retSchedules = null;
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.getStartTime() >= end || schedule.getEndTime() <= start) {
                continue;
            }
            if (retSchedules == null) {
                retSchedules = new ArrayList();
            }
            retSchedules.add(schedule);
        }
        return retSchedules;
    }
            
    private void _addSchedule(long start, long end, long bw) throws BandwidthCalendarException {
        if (bw > this.capacity) {
            throw new BandwidthCalendarException(String.format("add bandwidth:%d -> over capacity:%d", bw, this.capacity));
        }
        for (int index = 0; index < schedules.size(); index++) {
            BandwidthSchedule schedule = schedules.get(index);
            if (schedule.getStartTime() >= end) {
                schedules.add(index, new BandwidthSchedule(start, end, bw));
                return;
            }
        }
        schedules.add(new BandwidthSchedule(start, end, bw));
    }

    private void _removeSchedule(long start, long end) throws BandwidthCalendarException {
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.getStartTime() == start && schedule.getEndTime() == end) {
                it.remove();
                return;
            }
        }
        throw new BandwidthCalendarException(String.format("unknown schdule (%d, %d)", start, end));
    }
    

    private boolean _reduceSchedule(long start, long end, long bw) throws BandwidthCalendarException {
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.getStartTime() == start && schedule.getEndTime() == end) {
                boolean ret = schedule.reduceBandwidth(bw);
                if (schedule.getBandwidth() == 0) {
                    it.remove();
                }
                return ret;
            }
        }
        throw new BandwidthCalendarException(String.format("unknown schdule (%d, %d)", start, end));
    }
    
    public void addSchedule(long start, long end, long bw) throws BandwidthCalendarException {
        List<BandwidthSchedule> overlappedSchedules = lookupSchedulesBetween(start, end);
        if (overlappedSchedules == null || overlappedSchedules.isEmpty()) {
            this._addSchedule(start, end, bw);
            return;
        }
        int overlapIndex = schedules.indexOf(overlappedSchedules.get(0));
        BandwidthSchedule first = schedules.get(overlapIndex);
        if (first.getStartTime() < start) {
            BandwidthSchedule dupFirst = first.clone();
            first.setStartTime(start);
            dupFirst.setEndTime(start);
            schedules.add(overlapIndex, dupFirst);
            overlapIndex++;
        }
        int lastOverlap = overlapIndex + overlappedSchedules.size()-1;
        BandwidthSchedule last = schedules.get(lastOverlap);
        if (last.getEndTime() > end) {
            BandwidthSchedule dupLast = last.clone();
            last.setEndTime(end);
            dupLast.setStartTime(end);
            schedules.add(lastOverlap+1, dupLast);
        }        
        long gapStart = start;
        while (overlapIndex <= lastOverlap) {
            BandwidthSchedule overlapSchedule = schedules.get(overlapIndex);
            if (overlapSchedule.getStartTime() > gapStart) {
                this._addSchedule(gapStart, overlapSchedule.getStartTime(), bw);
                overlapIndex++;
            } 
            gapStart = overlapSchedule.getEndTime();
            if (overlapSchedule.getBandwidth() + bw > this.capacity) {
                throw new BandwidthCalendarException(String.format("add bandwidth:%d to %s -> over capacity:%d",
                        bw, overlapSchedule, this.capacity));
            }
            overlapSchedule.addBandwidth(bw);
            overlapIndex++;
        }
        if (gapStart < end) {
            this._addSchedule(gapStart, end, bw);
        }
    }

    //@TODO: reduceSchedule

    public void combineSchedule(long start, long end, long bw) throws BandwidthCalendarException {
        List<BandwidthSchedule> overlappedSchedules = lookupSchedulesBetween(start, end);
        if (overlappedSchedules == null || overlappedSchedules.isEmpty()) {
            this._addSchedule(start, end, bw);
            return;
        }
        int overlapIndex = schedules.indexOf(overlappedSchedules.get(0));
        BandwidthSchedule first = schedules.get(overlapIndex);
        if (first.getStartTime() < start) {
            BandwidthSchedule dupFirst = first.clone();
            first.setStartTime(start);
            dupFirst.setEndTime(start);
            schedules.add(overlapIndex, dupFirst);
            overlapIndex++;
        }
        int lastOverlap = overlapIndex + overlappedSchedules.size()-1;
        BandwidthSchedule last = schedules.get(lastOverlap);
        if (last.getEndTime() > end) {
            BandwidthSchedule dupLast = last.clone();
            last.setEndTime(end);
            dupLast.setStartTime(end);
            schedules.add(lastOverlap+1, dupLast);
        }
        long gapStart = start;
        while (overlapIndex <= lastOverlap) {
            BandwidthSchedule overlapSchedule = schedules.get(overlapIndex);
            if (overlapSchedule.getStartTime() > gapStart) {
                this._addSchedule(gapStart, overlapSchedule.getStartTime(), bw);
                overlapIndex++;
            } 
            gapStart = overlapSchedule.getEndTime();
            overlapSchedule.setBandwidth(overlapSchedule.getBandwidth() > bw ? overlapSchedule.getBandwidth(): bw);
            overlapIndex++;
        }
        if (gapStart < end) {
            this._addSchedule(gapStart, end, bw);
        }
    }
    
    public void combineCalendar(BandwidthCalendar bwCalendar) throws BandwidthCalendarException {
        // combine capacity to the min 
        if (bwCalendar.getCapacity() < this.capacity) {
            this.setCapacity(bwCalendar.getCapacity());
        }
        // combine schedules to the max
        ListIterator<BandwidthSchedule> it = bwCalendar.schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            this.combineSchedule(schedule.getStartTime(), schedule.getEndTime(), schedule.getBandwidth());
        }
    }
    
    public BandwidthCalendar residual(long start, long end) {
        BandwidthCalendar residual = new BandwidthCalendar(this.capacity);
        if (schedules.isEmpty()) {
            residual.schedules.add(new BandwidthSchedule(new Date().getTime()/1000L, infinite, this.capacity));
            return residual;
        }
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        long lastEnd = start;
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (lastEnd > 0 && lastEnd < schedule.startTime) {
                BandwidthSchedule residualScheduleGap = new BandwidthSchedule(lastEnd, schedule.startTime, this.capacity);
                residual.schedules.add(residualScheduleGap);
            }
            lastEnd = schedule.getEndTime();
            BandwidthSchedule residualSchedule = schedule.clone();
            residualSchedule.setBandwidth(capacity - schedule.getBandwidth());
            if (residualSchedule.getBandwidth() > 0) {
                residual.schedules.add(residualSchedule);
            }
            if (!it.hasNext() && schedule.getEndTime() < end) {
                BandwidthSchedule residualScheduleTail = new BandwidthSchedule(schedule.getEndTime(), infinite, this.capacity);
                residual.schedules.add(residualScheduleTail);
            }
        }
        return residual;
    }
    
    
    public BandwidthCalendar clone() {
        BandwidthCalendar clone = new BandwidthCalendar(this.capacity);
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            clone.schedules.add(it.next());
        }
        return clone;
    }
    
    public String toString() {
        String ret = "BandwidthCalendar:{";
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            ret += it.next().toString();
            if (it.hasNext()) {
                ret += " - ";
            }
        }
        return ret + "}";
    }
    
    public static BandwidthSchedule makePathBandwidthSchedule(Model model, MCETools.Path path, JSONObject options) throws BandwidthCalendarException {
        BandwidthCalendar pathAvailBwCal = null;
        Iterator<Statement> itS = path.iterator();
        while (itS.hasNext()) {
            Statement link = itS.next();
            BandwidthCalendar hopBwCal = getHopBandwidthCalendar(model, path, link.getSubject().asResource()); 
            if (pathAvailBwCal == null && hopBwCal != null) {
                pathAvailBwCal = hopBwCal.clone();
            } else if (hopBwCal != null) {
                pathAvailBwCal.combineCalendar(hopBwCal);
            }
            if (!itS.hasNext()) {
                hopBwCal = getHopBandwidthCalendar(model, path, link.getObject().asResource());
                if (pathAvailBwCal == null && hopBwCal != null) {
                    pathAvailBwCal = hopBwCal.clone();
                } else if (hopBwCal != null) {
                    pathAvailBwCal.combineCalendar(hopBwCal);
                }
            }
        }
        if (pathAvailBwCal == null) {
            return null;
        }
        // find schedule 'path.bandwithScedule' with 'pathAvailBwCal' based on 'options'
        // scenario 1: fixed [start - end] or (start:duration) w/o options
        // scenario 2: slinding [start - end > duration) w/ options
        // scenario 3: time-bandwidth-product w/ options
        long start = path.getBandwithScedule().getStartTime();
        long end = path.getBandwithScedule().getEndTime();
        long duration = end - start; 
        long deadline = duration; // offset from start
        BandwidthSchedule schedule;
        if (options.containsKey("tbp-mbytes")) {
            String strProductMbytes = (String)options.get("tbp-mbytes");
            Long timeBandwidthProductBits = Long.parseLong(strProductMbytes)*8000000;
            List<BandwidthSchedule> boxedSchedules = pathAvailBwCal.getTimeBandwidthProductSchedules(timeBandwidthProductBits, start, deadline);
            //@TODO: handle cases with granularity > 1
            if (options.containsKey("use-highest-bandwidth") && ((String)options.get("use-highest-bandwidth")).equalsIgnoreCase("true")) {
                // find the maximum bandwidth schedule in boxedSchedules (adjust size)
                long boxBw = 0;
                BandwidthSchedule selectedSchedule = null;
                for (BandwidthSchedule boxedSchedule: boxedSchedules) {
                    // find a schedule in boxedSchedules with bandwidth between these maximum and minimum (adjust size)
                    if (boxedSchedule.getBandwidth() > boxBw) {
                        boxBw = boxedSchedule.getBandwidth();
                        selectedSchedule = boxedSchedule;
                    }
                }
                if (selectedSchedule == null) {
                    return null;
                }
                long boxStart = selectedSchedule.getStartTime();
                long boxEnd = (long)Math.ceil((double)timeBandwidthProductBits / boxBw) + boxStart;
                schedule = new BandwidthSchedule(boxStart, boxEnd, boxBw);
            } else if (options.containsKey("use-lowest-bandwidth") && ((String)options.get("use-lowest-bandwidth")).equalsIgnoreCase("true")) {
                // find widest schedule in boxedSchedules (adjust size)
                long scheduleWidth = 0;
                BandwidthSchedule selectedSchedule = null;
                for (BandwidthSchedule boxedSchedule: boxedSchedules) {
                    // find a schedule in boxedSchedules with bandwidth between these maximum and minimum (adjust size)
                    if (boxedSchedule.getEndTime() - boxedSchedule.getStartTime() > scheduleWidth) {
                        scheduleWidth = boxedSchedule.getEndTime() - boxedSchedule.getStartTime();
                        selectedSchedule = boxedSchedule;
                    }
                }
                if (selectedSchedule == null) {
                    return null;
                }
                long boxStart = selectedSchedule.getStartTime();
                long boxEnd = selectedSchedule.getEndTime();
                schedule = new BandwidthSchedule(boxStart, boxEnd, (long)Math.ceil((double)timeBandwidthProductBits / scheduleWidth));
            } else if (options.containsKey("bandwidth-mbps <=") && options.get("bandwidth-mbps <=") != null) {
                String strBwMbps = (String)options.get("bandwidth-mbps <=");
                Long maxBwBps = Long.parseLong(strBwMbps)*1000000;                
                Long minBwBpsLeast = (long)Math.ceil((double)timeBandwidthProductBits / (end-start));
                if (maxBwBps < minBwBpsLeast) {
                    maxBwBps = minBwBpsLeast;
                }
                Long minBwBps = minBwBpsLeast;
                if (options.containsKey("bandwidth-mbps >=") && options.get("bandwidth-mbps >=") != null) {
                    strBwMbps = (String)options.get("bandwidth-mbps >=");
                    minBwBps = Long.parseLong(strBwMbps)*1000000;
                    if (minBwBps < minBwBpsLeast) {
                        minBwBps = minBwBpsLeast;
                    }
                }
                schedule = null;
                for (BandwidthSchedule boxedSchedule: boxedSchedules) {
                    if (boxedSchedule.getBandwidth() < minBwBps) {
                        continue;
                    }
                    // find a schedule in boxedSchedules with bandwidth between these maximum and minimum (adjust size)
                    if (boxedSchedule.getBandwidth() <= maxBwBps) {
                        long boxBw = boxedSchedule.getBandwidth();
                        long boxStart = boxedSchedule.getStartTime();
                        long boxEnd = (long)Math.ceil((double)timeBandwidthProductBits / boxBw + boxStart);
                        schedule = new BandwidthSchedule(boxStart, boxEnd, boxBw);
                        break;
                    } else if (maxBwBps * (boxedSchedule.getEndTime()- boxedSchedule.getStartTime()) >= timeBandwidthProductBits) {
                        long boxBw = maxBwBps;
                        long boxStart = boxedSchedule.getStartTime();
                        long boxEnd = (long)Math.ceil((double)timeBandwidthProductBits / boxBw + boxStart);
                        schedule = new BandwidthSchedule(boxStart, boxEnd, boxBw);
                        break;
                    }
                }
            } else {
                return null;
            }
        } else {
            if (options.containsKey("sliding-duration")) {
                duration = (long)options.get("sliding-duration");
            } 
            schedule = pathAvailBwCal.makeSchedule(path.getBandwithScedule().getBandwidth(), start, duration, deadline, false);
        }
        // scenarios: extra options / queries
        return schedule;    
    }
    
    //$$ normalize bandwidth unit internally
    private static BandwidthCalendar getHopBandwidthCalendar(Model model, MCETools.Path path, Resource hop) throws BandwidthCalendarException {
        String sparql = "SELECT ?capacity ?unit WHERE {"
                + String.format("<%s> a nml:BidirectionalPort; nml:hasService ?bwSvc .  ", hop.getURI())
                + "?bwSvc a mrs:BandwidthService. "
                + "?bwSvc mrs:reservableCapacity ?capacity. "
                + "OPTIONAL { ?bwSvc mrs:unit ?unit }"
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        BandwidthCalendar bwCal = null;
        if (rs.hasNext()) {
            QuerySolution solution = rs.next();
            long bw = Long.parseLong(solution.getLiteral("capacity").getString());
            String unit = solution.contains("unit") ? solution.getLiteral("unit").getString() : "bps";
            bw = MCETools.normalizeBandwidth(bw, unit);
            bwCal = new BandwidthCalendar(bw);
        }
        if (bwCal == null) {
            return null;
        }

        sparql = "SELECT ?subport ?start ?end WHERE {"
                + String.format("<%s> nml:hasBidirectionalPort ?subport. ", hop.getURI())
                + "?subport nml:hasService ?subBwSvc. "
                + "?subBwSvc a mrs:BandwidthService. "
                + "OPTIONAL { ?subBwSvc nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end. } "
                + "}";
        rs = ModelUtil.sparqlQuery(model, sparql);
        while (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Resource resSubport = solution.getResource("subport");
            MCETools.BandwidthProfile subBwProfile = MCETools.getHopBandwidthPorfile(model, resSubport);
            if (subBwProfile == null || subBwProfile.reservableCapacity == null) {
                continue;
            }
            long start = new Date().getTime()/1000L;
            long end = infinite;
            if (solution.contains("start")) {
                try {
                    start = DateTimeUtil.getBandwidthScheduleSeconds(solution.getLiteral("start").getString());
                } catch (Exception ex) {
                    throw new BandwidthCalendarException("cannot parse schedule start time: " + solution.getLiteral("start").getString());
                } 
            }
            if (solution.contains("end")) {
                try {
                    end= DateTimeUtil.getBandwidthScheduleSeconds(solution.getLiteral("end").getString());
                } catch (Exception ex) {
                    throw new BandwidthCalendarException("cannot parse schedule end time: " + solution.getLiteral("end").getString());
                }
            }
            bwCal.addSchedule(start, end, subBwProfile.reservableCapacity);
        }
        return bwCal;
    }
    

    // deadline is a offset value to be relative to startTime (consistent 'now')
    public BandwidthSchedule makeSchedule(long bw, long start, long duration, long deadline, boolean add) {
        BandwidthCalendar residual = this.residual(start, start+deadline);
        ListIterator<BandwidthSchedule> it = residual.getSchedules().listIterator();
        // remove all segments without required bandwidth
        while (it.hasNext()) {
            BandwidthSchedule residualSchedule = it.next();
            if (residualSchedule.getEndTime() <= start || !residualSchedule.reduceBandwidth(bw)) {
                it.remove();
            }
        }
        // now look for the continuous chunk before deadline
        it = residual.getSchedules().listIterator();
        BandwidthSchedule retSchedule = null;
        long continuousStart = start;
        long continuousEnd = start;
        if (deadline > 0) {
            deadline += start;
        } else {
            deadline = infinite;
        }
        while (it.hasNext()) {
            BandwidthSchedule goodSchedule = it.next();
            if (continuousStart == start  && continuousEnd == start && goodSchedule.getStartTime() > start) {
                // align continuousStart to first time slot
                continuousStart = goodSchedule.getStartTime();
            }
            if (continuousEnd != start && continuousEnd != goodSchedule.getStartTime()) {
                // reset continuousStart if gap
                continuousStart = goodSchedule.getStartTime();
            }
            continuousEnd = goodSchedule.getEndTime();
            if ((continuousEnd <= deadline && continuousEnd - continuousStart >= duration)
                    || (continuousEnd > deadline && deadline - continuousStart >= duration)){
                retSchedule = new BandwidthSchedule(continuousStart, continuousStart + duration, bw);
                break;
            }
        }
        if (retSchedule != null && add) {
            try {
                this.addSchedule(retSchedule.getStartTime(), retSchedule.getEndTime(), retSchedule.getBandwidth());
            } catch (BandwidthCalendarException ex) {
                return null; // this should never happen
            }
        }
        return retSchedule;
    }

    // deadline is a offset value to be relative to startTime (consistent 'now')
    public List<BandwidthSchedule> getTimeBandwidthProductSchedules(long tbp, long start, long deadline) {
        List<BandwidthSchedule> retScheduleList = new ArrayList();
        if (deadline > 0) {
            deadline += start;
        } else {
            deadline = infinite;
        }
        BandwidthCalendar residual = this.residual(start, deadline);
        ListIterator<BandwidthSchedule> it = residual.getSchedules().listIterator();
        while (it.hasNext()) {
            BandwidthSchedule residualSchedule = it.next();
            if (residualSchedule.getEndTime() <= start) {
                it.remove();
            }
            if (residualSchedule.getStartTime() >= deadline) {
                it.remove();
            }
        }
        long continuousStart = start;
        long continuousEnd = start;
        List<BandwidthSchedule> trialSchedules = new ArrayList();
        it = residual.getSchedules().listIterator();
        while (it.hasNext()) {
            BandwidthSchedule goodSchedule = it.next();
            trialSchedules.add(goodSchedule);
            if (continuousStart == start  && continuousEnd == start && goodSchedule.getStartTime() > start) {
                // align continuousStart to first time slot
                continuousStart = goodSchedule.getStartTime();
            }
            if (continuousEnd != start && continuousEnd != goodSchedule.getStartTime()) {
                // reset continuousStart if gap
                continuousStart = goodSchedule.getStartTime();
                trialSchedules.clear();
            }
            continuousEnd = goodSchedule.getEndTime();
            if (continuousEnd > deadline){
                continuousEnd = deadline;
            }
            // starting from trialStart, go through all time marks of trialSchedules and determine the box size including its left and right
            int numSegs = trialSchedules.size();
            //Map<Long, Long> timeBandwidthMap = new HashMap();
            //timeBandwidthMap.put(continuousStart, trialSchedules.get(0).getBandwidth());
            //timeBandwidthMap.put(continuousEnd, trialSchedules.get(numSegs-1).getBandwidth());
            if (numSegs == 1) {
                BandwidthSchedule bsx = trialSchedules.get(0);
                long boxStart = bsx.getStartTime() < continuousStart ? continuousStart : bsx.getStartTime();
                long boxEnd = bsx.getEndTime() > continuousEnd ? continuousEnd : bsx.getEndTime();
                long boxBw = bsx.getBandwidth();
                if (boxBw * (boxEnd - boxStart) > tbp) {
                    BandwidthSchedule boxSchedule = new BandwidthSchedule(boxStart, boxEnd, boxBw);
                    retScheduleList.add(boxSchedule);
                }
            } else {
                // add all right-boxed bandwidth
                for (int x = 1; x < numSegs; x++) {
                    BandwidthSchedule bsx = trialSchedules.get(x);
                    int y = x - 1;
                    for (; y >= 0; y--) {
                        BandwidthSchedule bsy = trialSchedules.get(y);
                        if (bsy.getBandwidth() < bsx.getBandwidth()) {
                            break;
                        }
                    }
                    int z = x + 1;
                    for (; z < numSegs; z++) {
                        BandwidthSchedule bsz = trialSchedules.get(z);
                        if (bsz.getBandwidth() < bsx.getBandwidth()) {
                            break;
                        }
                    }
                    // now we got the box btween y+1 and z-1
                    BandwidthSchedule bsyplus = trialSchedules.get(y + 1);
                    BandwidthSchedule bszminus = trialSchedules.get(z - 1);
                    long boxStart = bsyplus.getStartTime() < continuousStart ? continuousStart : bsyplus.getStartTime();
                    long boxEnd = bszminus.getEndTime() > continuousEnd ? continuousEnd : bszminus.getEndTime();
                    long boxBw = bsx.getBandwidth();
                    if (boxBw * (boxEnd - boxStart) > tbp) {
                        BandwidthSchedule boxSchedule = new BandwidthSchedule(boxStart, boxEnd, boxBw);
                        retScheduleList.add(boxSchedule);
                    }
                }
            }
        }
        return retScheduleList;
    }

}