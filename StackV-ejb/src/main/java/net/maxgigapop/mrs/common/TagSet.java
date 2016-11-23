/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.common;

import java.util.ArrayList;
import java.util.Random;
import javax.ejb.EJBException;

/**
 *
 * @author xyang
 */
public class TagSet {

    public static final Integer MAX_VLAN = 4096;
    public static final String VLAN_ANY_RANGE = "2-4094";
    public static TagSet VlanRangeANY = new TagSet(TagSet.VLAN_ANY_RANGE);
    private int base;
    private int interval;
    private boolean[] map;

    public static class EmptyTagSetExeption extends Exception {

        @Override
        public String getMessage() {
            return "TagSet is empty .";
        }
    }

    public static class NoneVlanExeption extends Exception {

        @Override
        public String getMessage() {
            return "Found none VLAN resource.";
        }
    }

    private void init(int num, int base, int interval) {
        map = new boolean[num];
        for (int i = 0; i < num; i++) {
            map[i] = false;
        }
        this.base = base;
        this.interval = interval;
    }

    public TagSet() {
        init(MAX_VLAN, 0, 1);
    }

    public TagSet(int num, int base, int interval) {
        init(num, base, interval);
    }

    // VLAN range only
    public TagSet(String range) {
        init(MAX_VLAN, 0, 1);

        if (range == null) {
            return;
        }
        range = range.trim();
        if (range.equals("")) {
            return;
        }

        if (range.toLowerCase().equals("any")) {
            range = VLAN_ANY_RANGE;
        }

        String[] rangeList = range.split(",");
        try {
            for (int i = 0; i < rangeList.length; i++) {
                if (rangeList[i].trim().equals("")) {
                    continue;
                }
                String[] rangeEnds = rangeList[i].trim().split("-");
                if (rangeEnds.length == 1) {
                    int tag = Integer.parseInt(rangeEnds[0].trim());
                    map[tag] = true;
                } else if (rangeEnds.length == 2 && "".equals(rangeEnds[0])) {
                    int tag = Integer.parseInt(rangeEnds[1].trim());
                    map[tag] = true;
                } else if (rangeEnds.length == 2) {
                    int start = Integer.parseInt(rangeEnds[0].trim());
                    int end = Integer.parseInt(rangeEnds[1].trim());
                    if (end < start) {
                        throw new RuntimeException("Invalid range: end < start: " + range);
                    }
                    for (int k = start; k <= end; k++) {
                        map[k] = true;
                    }
                }
            }
        } catch (NumberFormatException ex) {
            throw new EJBException("Invalid VLAN range format: " + ex.getMessage());
        }
    }

    public TagSet clone() {
        TagSet tagset = new TagSet(map.length, base, interval);
        for (int i = 0; i < map.length; i++) {
            tagset.map[i] = this.map[i];
        }
        return tagset;
    }

    public boolean isEmpty() {
        for (int i = 0; i < map.length; i++) {
            if (map[i]) {
                return false;
            }
        }
        return true;
    }

    public int getFirst() {
        for (int i = 0; i < map.length; i++) {
            if (map[i]) {
                return i;
            }
        }
        return -1;
    }

    public int getRandom() {
        ArrayList<Integer> choices = new ArrayList<Integer>();
        for (int i = 0; i < map.length; i++) {
            if (map[i]) {
                choices.add(i);
            }
        }

        if (choices.isEmpty()) {
            return -1;
        }

        Random rand = new Random();
        return choices.get(rand.nextInt(choices.size()));
    }

    public String toString() {
        String range = "";
        int start = 0;

        for (int i = 0; i < map.length; i++) {
            if (map[i]) {
                start = i;
                break;
            }
        }

        if (start == (map.length - 1)) {
            return range;
        }

        ArrayList<int[]> fragments = new ArrayList<int[]>();
        int[] fragment = new int[2];
        fragment[0] = start;
        fragment[1] = map.length - 1;
        boolean prev = true;
        for (int i = start + 1; i < map.length; i++) {
            if (prev != map[i]) {
                if (prev) {
                    fragment[1] = i - 1;
                    fragments.add(fragment);
                } else {
                    fragment = new int[2];
                    fragment[0] = i;
                }
            }
            prev = map[i];
        }

        for (int i = 0; i < fragments.size(); i++) {
            int[] tmp = fragments.get(i);
            if (tmp[0] == tmp[1]) {
                range += tmp[0] * interval;
            } else {
                range += (tmp[0] * interval) + "-" + (tmp[1] * interval);
            }
            if (i < fragments.size() - 1) {
                range += ",";
            }
        }

        return range;
    }

    public boolean hasTag(int tag) {
        tag = (tag - base) / interval;
        if (tag >= map.length) {
            return false;
        }
        return (map[tag]);
    }

    public void addTag(int tag) {
        tag = (tag - base) / interval;
        map[tag] = true;
    }

    public void removeTag(int tag) {
        tag = (tag - base) / interval;
        map[tag] = false;
    }

    public void setAny() {
        for (int i = 0; i < map.length; i++) {
            map[i] = true;
        }
    }

    public void intersect(TagSet tagset) {
        for (int i = 0; i < map.length; i++) {
            this.map[i] = (this.map[i] && tagset.map[i]);
        }
    }

    public void join(TagSet tagset) {
        for (int i = 0; i < map.length; i++) {
            this.map[i] = (this.map[i] || tagset.map[i]);
        }
    }
}
