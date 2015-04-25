package eecs547.tac.aa.v3;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Augie
 */
public class OpponentModel {

    private static int INDEX_GENERIC = 0, INDEX_TARGETED = 1;
    private GameModel gM;
    private String name;
    private HashMap<Query, Integer[]> targetedAdBuckets = new HashMap<Query, Integer[]>();
    private HashMap<Query, ArrayList<Double>> positions = new HashMap<Query, ArrayList<Double>>();

    public OpponentModel(String opponent, GameModel gM) {
        this.name = opponent;
        this.gM = gM;
        // initialize data structures
        for (Query q : gM.agent.querySpace) {
            targetedAdBuckets.put(q, new Integer[]{0, 0});
            positions.put(q, new ArrayList<Double>());
        }
    }

    public void handleAd(Query q, Ad ad) {
        if (ad != null && ad.isGeneric()) {
            targetedAdBuckets.get(q)[INDEX_GENERIC]++;
        } else {
            targetedAdBuckets.get(q)[INDEX_TARGETED]++;
        }
    }

    public void handlePosition(Query q, double position) {
        positions.get(q).add(position);
    }

    public void handleQueryReport(Query q, QueryReport queryReport) {
        handleAd(q, queryReport.getAd(q, name));
        handlePosition(q, queryReport.getPosition(q, name));
    }

    public int predictPosition(Query query) {
        return (int) Math.round(positions.get(query).get(positions.get(query).size() - 1));
    }

    public boolean predictTargeted(Query query) {
        return prTargeted(query) > 0.5d;
    }

    public double prTargeted(Query query) {
        Integer[] arr = targetedAdBuckets.get(query);
        if (arr[INDEX_GENERIC] + arr[INDEX_TARGETED] == 0) {
            return 0;
        }
        return (double) arr[INDEX_TARGETED] / (double) (arr[INDEX_GENERIC] + arr[INDEX_TARGETED]);
    }
}
