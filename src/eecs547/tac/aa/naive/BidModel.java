package eecs547.tac.aa.naive;

import eecs547.tac.aa.Bid;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import java.util.HashMap;

/**
 *
 * @author Augie
 */
public class BidModel {

    protected NaiveTigerBlood agent;
    private HashMap<Query, BidOptimizer> bidders = new HashMap<Query, BidOptimizer>();

    public BidModel(NaiveTigerBlood agent) {
        this.agent = agent;
    }

    public Bid getBid(Query query) {
        return getBidder(query).getBid();
    }

    public BidOptimizer getBidder(Query query) {
        if (!bidders.containsKey(query)) {
            bidders.put(query, new BidOptimizer(this, query));
        }
        return bidders.get(query);
    }

    public double getDailySpendLimit() {
        return BidBundle.NO_SPEND_LIMIT;
    }

    public int getUsedCapacity() {
        int used = 0;
        for (int i = 0; i < agent.advertiserInfo.getDistributionWindow(); i++) {
            for (BidOptimizer bo : bidders.values()) {
                used += bo.getConversions(i);
            }
        }
        return used;
    }

    public void handleQueryReport(QueryReport queryReport) {
        for (Query q : agent.querySpace) {
            getBidder(q).handleQueryReport(queryReport);
        }
    }

    public void handleSalesReport(SalesReport salesReport) {
        for (Query q : agent.querySpace) {
            getBidder(q).handleSalesReport(salesReport);
        }
    }
}
