package eecs547.tac.aa.naive;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author Augie
 */
public class UserModel {

    private GameModel gM;
    private Map<Query, LinkedList<Integer>> impressions = new HashMap<Query, LinkedList<Integer>>();

    public UserModel(GameModel gM) {
        this.gM = gM;
    }

    public LinkedList<Integer> getImpressions(Query q) {
        if (!impressions.containsKey(q)) {
            impressions.put(q, new LinkedList<Integer>());
        }
        return impressions.get(q);
    }

    public void handleQueryReport(QueryReport queryReport) {
        for (Query q : gM.agent.querySpace) {
            getImpressions(q).addFirst(queryReport.getImpressions(q));
        }
    }

    public int predictImpressions(Query q) {
        return impressions.get(q).getFirst();
    }

    public int predictTotalImpressions() {
        int t = 0;
        for (Query q : impressions.keySet()) {
            if (!impressions.get(q).isEmpty()) {
                t += impressions.get(q).getFirst();
            }
        }
        return t;
    }
}
