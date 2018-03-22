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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.DateTimeUtil;
import net.maxgigapop.mrs.common.ModelUtil;
import org.json.simple.JSONObject;

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
            if (schedule.getStartTime() >= end || schedule.getEndTime() < start) {
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
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.getStartTime() >= end) {
                it.add(new BandwidthSchedule(start, end, bw));
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
            overlapIndex = 1;
        }
        int lastOverlap = overlapIndex + overlappedSchedules.size()-1;
        BandwidthSchedule last = schedules.get(lastOverlap);
        if (last.getEndTime() > end) {
            BandwidthSchedule dupLast = last.clone();
            last.setEndTime(end);
            dupLast.setStartTime(end);
            schedules.add(lastOverlap+1, dupLast);
        }
        
        while (overlapIndex <= lastOverlap) {
            BandwidthSchedule overlapSchedule = schedules.get(overlapIndex);
            if (overlapSchedule.getStartTime() > start) {
                this._addSchedule(start, overlapSchedule.getStartTime(), bw);
            } 
            if (overlapSchedule.getEndTime() < end) {
                this._addSchedule(overlapSchedule.getEndTime(), end, bw);
            }
            if (overlapSchedule.getBandwidth() + bw > this.capacity) {
                throw new BandwidthCalendarException(String.format("add bandwidth:%d to %s -> over capacity:%d",
                        bw, overlapSchedule, this.capacity));
            }
            overlapSchedule.addBandwidth(bw);
            overlapIndex++;
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
            overlapIndex = 1;
        }
        int lastOverlap = overlapIndex + overlappedSchedules.size()-1;
        BandwidthSchedule last = schedules.get(lastOverlap);
        if (last.getEndTime() > end) {
            BandwidthSchedule dupLast = last.clone();
            last.setEndTime(end);
            dupLast.setStartTime(end);
            schedules.add(lastOverlap+1, dupLast);
        }
        
        while (overlapIndex <= lastOverlap) {
            BandwidthSchedule overlapSchedule = schedules.get(overlapIndex);
            if (overlapSchedule.getStartTime() > start) {
                this._addSchedule(start, overlapSchedule.getStartTime(), bw);
            } 
            if (overlapSchedule.getEndTime() < end) {
                this._addSchedule(overlapSchedule.getEndTime(), end, bw);
            }
            overlapSchedule.setBandwidth(overlapSchedule.getBandwidth() > bw ? overlapSchedule.getBandwidth(): bw);
            overlapIndex++;
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
    
    public BandwidthSchedule makeSchedule(long bw, long duration, long deadline, boolean add) {
        if (deadline <= 0) {
            deadline = infinite;
        }
        BandwidthCalendar residual = this.residual();
        ListIterator<BandwidthSchedule> it = residual.getSchedules().listIterator();
        long now = new Date().getTime()/1000L;
        // remove all segments without required bandwidth
        while (it.hasNext()) {
            BandwidthSchedule residualSchedule = it.next();
            if (residualSchedule.getEndTime() <= now || !residualSchedule.reduceBandwidth(bw)) {
                it.remove();
            }
        }
        // now look for the continuous chunk before deadline
        it = residual.getSchedules().listIterator();
        BandwidthSchedule retSchedule = null;
        long continuousStart = now;
        long continuousEnd = now;
        while (it.hasNext()) {
            BandwidthSchedule goodSchedule = it.next();
            if (continuousStart == now  || continuousEnd != goodSchedule.getStartTime()) {
                continuousStart = (goodSchedule.getStartTime() > now ? goodSchedule.getStartTime() : now);
                continuousEnd = goodSchedule.getEndTime();
            } else {
                continuousEnd = goodSchedule.getEndTime();
            }
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

    public BandwidthCalendar residual() {
        BandwidthCalendar residual = new BandwidthCalendar(this.capacity);
        if (schedules.isEmpty()) {
            residual.schedules.add(new BandwidthSchedule(new Date().getTime()/1000L, infinite, this.capacity));
            return residual;
        }
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        long lastEnd = 0;
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (lastEnd > 0 && lastEnd < schedule.startTime) {
                BandwidthSchedule residualScheduleGap = new BandwidthSchedule(lastEnd, schedule.startTime, this.capacity);
                residual.schedules.add(residualScheduleGap);
            }
            BandwidthSchedule residualSchedule = schedule.clone();
            residualSchedule.setBandwidth(capacity - schedule.getBandwidth());
            residual.schedules.add(residualSchedule);
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
        long start = path.getBandwithScedule().getStartTime();
        long end = path.getBandwithScedule().getEndTime();
        long duration = end - start;
        long deadline = end;
        if (options.containsKey("sliding-duration")) {
            duration = (long)options.get("sliding-duration");
        } 
        BandwidthSchedule schedule = pathAvailBwCal.makeSchedule(start, duration, deadline, false);
        // scenarios: extra options / queries
        return schedule;    
    }
    
    //$$ normalize bandwidth unit internally
    private static BandwidthCalendar getHopBandwidthCalendar(Model model, MCETools.Path path, Resource hop) throws BandwidthCalendarException {
        String sparql = "SELECT ?subport ?start ?end WHERE {"
                + String.format("<%s> a nml:BidirectionalPort. ", hop.getURI())
                + String.format("<%s> nml:hasBidirectionalPort ?subport. ", hop.getURI())
                + "?subport nml:hasService ?subBwSvc. "
                + "?subBwSvc a mrs:BandwidthService. "
                + "OPTIONAL { ?subBwSvc nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end. } "
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        BandwidthCalendar bwCal = null;
        while (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Resource resSubport = solution.getResource("subport");
            MCETools.BandwidthProfile subBwProfile = MCETools.getHopBandwidthPorfile(model, resSubport);
            if (subBwProfile == null || subBwProfile.maximumCapacity == null || subBwProfile.reservableCapacity == null) {
                continue;
            }
            if (bwCal == null) {
                if (path.getBandwithProfile() != null && path.getBandwithProfile().reservableCapacity != null) {
                    bwCal = new BandwidthCalendar(path.getBandwithProfile().reservableCapacity);
                } else {
                    bwCal = new BandwidthCalendar(subBwProfile.maximumCapacity);
                }
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
}