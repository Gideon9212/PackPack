package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SequenceGroupHandler implements GroupHandler {
    public final String name;

    private final List<List<Integer>> sequenceGroup;
    private final ArrayList<EventGroup> groups = new ArrayList<>();

    public SequenceGroupHandler(@NotNull List<List<Integer>> sequenceGroup, @NotNull String name) {
        this.sequenceGroup = sequenceGroup;
        this.name = name;
    }

    @Override
    public boolean handleEvent(Schedule schedule) {
        if(schedule instanceof StageSchedule) {
            if(((StageSchedule) schedule).stages.isEmpty())
                return false;

            if(containsOnlyValidStages((StageSchedule) schedule)) {
                boolean added = false;

                for(EventGroup group : groups) {
                    if (group.finished)
                        continue;

                    if(sequenceCanBeApplied(group.schedules, (StageSchedule) schedule)) {
                        added |= group.addStage((StageSchedule) schedule);
                    }
                }

                if(!added) {
                    EventGroup newGroup = new EventGroup(sequenceGroup, name);

                    boolean result = newGroup.addStage((StageSchedule) schedule);

                    groups.add(newGroup);

                    return result;
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void clear() {
        groups.clear();
    }

    @Override
    public ArrayList<EventGroup> getGroups() {
        return groups;
    }

    private boolean containsOnlyValidStages(StageSchedule schedule) {
        for (List<Integer> group : sequenceGroup) {
            ArrayList<Integer> data = new ArrayList<>();

            for (StageMap stm : schedule.stages) {
                int id = EventFactor.stageToInteger(stm);

                if (id == -1 || !group.contains(id)) {
                    return false;
                } else {
                    data.add(id);
                }
            }

            if(data.containsAll(group))
                return true;
        }

        return false;
    }

    private boolean sequenceCanBeApplied(Schedule[] schedules, StageSchedule schedule) {
        if(schedules.length == 1 && schedules[0] != null)
            return false;

        for(int i = 0; i < schedules.length; i++) {
            if(schedules[i] != null) {
                StageSchedule sc = (StageSchedule) schedules[i];

                if(i == 0) {
                    if(sc.date.dateEnd.equals(schedule.date.dateStart) && schedules[i+1] == null)
                        return true;
                } else if(i == schedules.length - 1) {
                    if(sc.date.dateStart.equals(schedule.date.dateEnd) && schedules[i - 1] == null) {
                        return true;
                    }
                } else {
                    if(sc.date.dateStart.equals(schedule.date.dateEnd) && i > 1 && schedules[i - 1] == null) {
                        return true;
                    } else if(sc.date.dateEnd.equals(schedule.date.dateStart) && i < schedules.length - 1 && schedules[i + 1] == null)
                        return true;
                }
            }
        }

        return scheduleIsEmpty(schedules);
    }

    private boolean scheduleIsEmpty(Schedule[] schedules) {
        for(Schedule schedule : schedules) {
            if(schedule != null)
                return false;
        }

        return true;
    }
}
