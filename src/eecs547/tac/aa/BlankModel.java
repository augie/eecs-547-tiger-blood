package eecs547.tac.aa;

import uchicago.src.sim.engine.BaseController;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimEvent;
import uchicago.src.sim.engine.SimModelImpl;

/**
 *
 * @author Augie
 */
public class BlankModel extends SimModelImpl {

    private Schedule schedule;

    @Override
    public void begin() {
    }

    @Override
    public String[] getInitParam() {
        return null;
    }

    @Override
    public String getName() {
        return "Blank";
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public void setup() {
        if (schedule == null) {
            schedule = new Schedule(1);
        }
        schedule.scheduleActionBeginning(0, this, "step");
    }

    public void step() {
        try {
            Thread.sleep(9800);
        } catch (Exception e) {
            Util.debug(e);
        }
    }

    public void run() {
        PlainController control = new PlainController();
        setController(control);
        control.setExitOnExit(true);
        control.setModel(this);
        addSimEventListener(control);
        control.startSimulation();
    }

    /**
     * Why this class below?
     *
     * the reason we did that is because the repast "BatchController" had methods
     * in it that started GUI stuff.  this caused problems when we ssh'd into
     * another machine and run a job--when we tried to disconnect, the ssh
     * session would stay hung until the job was finished because the job needed
     * the X11-forwarding to be open to run.
     */
    private static class PlainController extends BaseController {

        private boolean exitonexit;

        public PlainController() {
            super();
            exitonexit = false;
        }

        public void startSimulation() {
            startSim();
        }

        public void stopSimulation() {
            stopSim();
        }

        public void exitSim() {
            exitSim();
        }

        public void pauseSimulation() {
            pauseSim();
        }

        @Override
        public boolean isBatch() {
            return true;
        }

        protected void onTickCountUpdate() {
        }

        /**
         * this might not be necessary
         * @param in_Exitonexit
         */
        @Override
        public void setExitOnExit(boolean in_Exitonexit) {
            exitonexit = in_Exitonexit;
        }

        public void simEventPerformed(SimEvent evt) {
            if (evt.getId() == SimEvent.STOP_EVENT) {
                stopSimulation();
            } else if (evt.getId() == SimEvent.END_EVENT) {
                if (exitonexit) {
                    System.exit(0);
                }
            } else if (evt.getId() == SimEvent.PAUSE_EVENT) {
                pauseSimulation();
            }
        }

        /**
         * function added because it is required for repast 2.2
         * @return
         */
        public long getRunCount() {
            return 0;
        }

        /**
         * function added because it is required for repast 2.2
         * @return
         */
        public boolean isGUI() {
            return false;
        }
    }
}
