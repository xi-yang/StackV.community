/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author xyang
 */
public class BandwidthCalendar {
    public static final long infinite = Long.MAX_VALUE; 
    public static class BandwidthException extends Exception {
        public BandwidthException(String message) {
            super(message);
        }        
    }

    public static class BandwidthSchedule {
        long startTime = 0L;
        long endTime = infinite;
        long bandwidth = 0;

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
            return String.format("%ld: (%ld, %ld)", bandwidth, startTime, endTime);
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
            
    private void _addSchedule(long start, long end, long bw) throws BandwidthException {
        if (bw > this.capacity) {
            throw new BandwidthException(String.format("add bandwidth:%ld -> over capacity:%ld", bw, this.capacity));
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

    private void _removeSchedule(long start, long end) throws BandwidthException {
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
            if (schedule.getStartTime() == start && schedule.getEndTime() == end) {
                it.remove();
                return;
            }
        }
        throw new BandwidthException(String.format("unknown schdule (%ld, %ld)", start, end));
    }
    

    private boolean _reduceSchedule(long start, long end, long bw) throws BandwidthException {
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
        throw new BandwidthException(String.format("unknown schdule (%ld, %ld)", start, end));
    }
    
    public void addSchedule(long start, long end, long bw) throws BandwidthException {
        List<BandwidthSchedule> overlappedSchedules = lookupSchedulesBetween(start, end);
        if (overlappedSchedules.isEmpty()) {
            this._addSchedule(start, end, bw);
            return;
        }
        int overlapIndex = 0;
        BandwidthSchedule first = schedules.get(0);
        if (first.getStartTime() < start) {
            BandwidthSchedule dupFirst = first.clone();
            first.setStartTime(start);
            dupFirst.setEndTime(start);
            schedules.add(0, dupFirst);
            overlapIndex = 1;
        }
        int lastOverlap = schedules.size();
        BandwidthSchedule last = schedules.get(schedules.size()-1);
        if (last.getEndTime() > end) {
            BandwidthSchedule dupLast = last.clone();
            last.setEndTime(end);
            dupLast.setStartTime(end);
            schedules.add(dupLast);
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
                throw new BandwidthException(String.format("add bandwidth:%ld to %s -> over capacity:%ld",
                        bw, overlapSchedule, this.capacity));
            }
            overlapSchedule.addBandwidth(bw);
        }
    }

    //@TODO: reduceSchedule
    
    public BandwidthSchedule checkBandwidthSchedule(long bw, long duration, long deadline, boolean add) {
        if (deadline <= 0) {
            deadline = infinite;
        }
        BandwidthCalendar residual = this.residual();
        ListIterator<BandwidthSchedule> it = residual.getSchedules().listIterator();
        long now = new Date().getTime();
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
            if (continuousEnd <= deadline && continuousEnd - continuousStart >= duration) {
                retSchedule = new BandwidthSchedule(continuousStart, continuousStart + duration, bw);
                break;
            }
        }
        if (retSchedule != null && add) {
            try {
                this.addSchedule(retSchedule.getStartTime(), retSchedule.getEndTime(), retSchedule.getBandwidth());
            } catch (BandwidthException ex) {
                ; // this cannot go wrong
            }
        }
        return retSchedule;
    }


    public BandwidthCalendar residual() {
        BandwidthCalendar residual = new BandwidthCalendar(this.capacity);
        ListIterator<BandwidthSchedule> it = schedules.listIterator();
        while (it.hasNext()) {
            BandwidthSchedule schedule = it.next();
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
    
    //@TODO: toString
}