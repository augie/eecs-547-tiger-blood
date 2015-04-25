package eecs547.tac.aa.v2;

import eecs547.tac.aa.Bid;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import eecs547.tac.aa.Util;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Augie
 */
public class BidModel {

    private static final double START_SETTING_BID_LIMITS_THRESHOLD = 0.25;
    protected TigerBloodV2 agent;
    private HashMap<Query, BidOptimizer> bidders = new HashMap<Query, BidOptimizer>();
    private boolean needToSetBidLimits = true;

    public BidModel(TigerBloodV2 agent) {
        this.agent = agent;
    }

    public Bid getBid(Query query) {
        if (needToSetBidLimits) {
            needToSetBidLimits = false;
            if (getBidder(query).bid != null) {
                // optimize capacity
                double percUsedCapacity = (double) getUsedCapacity() / (double) agent.advertiserInfo.getDistributionCapacity();
                if (percUsedCapacity >= START_SETTING_BID_LIMITS_THRESHOLD) {
                    // get total impressions
                    final int totalImpressions = agent.gM.uM.predictTotalImpressions();
                    // rank the bidders
                    List<BidOptimizer> rankBidders = new LinkedList<BidOptimizer>();
                    rankBidders.addAll(bidders.values());
                    // do not remove the F2 queries
                    for (Query q : agent.querySpace) {
                        if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
                            rankBidders.remove(bidders.get(q));
                        }
                    }
                    // rank the bidders by importance
                    Collections.sort(rankBidders, new Comparator<BidOptimizer>() {

                        public int compare(BidOptimizer bo1, BidOptimizer bo2) {
                            return -1 * Double.valueOf(bo1.getAvgCPConv() * ((double) agent.gM.uM.predictImpressions(bo1.query) / (double) totalImpressions)).compareTo(Double.valueOf(bo2.getAvgCPConv() * ((double) agent.gM.uM.predictImpressions(bo2.query) / (double) totalImpressions)));
                        }
                    });
                    // how many bidders to cut off?
                    double biddersToCutOff = (double) rankBidders.size() * (percUsedCapacity - START_SETTING_BID_LIMITS_THRESHOLD) * (1d / (1 - START_SETTING_BID_LIMITS_THRESHOLD));
                    if (biddersToCutOff > rankBidders.size()) {
                        biddersToCutOff = (double) rankBidders.size();
                    }
                    for (int i = 0; i < rankBidders.size(); i++) {
                        if (i < biddersToCutOff) {
                            rankBidders.get(i).bid.spendLimit = 1;
                        } else {
                            rankBidders.get(i).bid.spendLimit = BidBundle.NO_SPEND_LIMIT;
                        }
                    }
                    Util.debug("Used Capacity: " + getUsedCapacity() + ", % Used Capacity: " + Util.round3(percUsedCapacity) + ", Bidders to cut off: " + biddersToCutOff + "/" + rankBidders.size());
                    // still a problem?
                    if (biddersToCutOff >= (double) rankBidders.size() - 1d) {
                        // reduce the F2 queries that are not the specialization
                        for (Query q : agent.querySpace) {
                            if (!q.equals(agent.getSpecializationQuery()) && q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
                                // half of the average amount spent on that query
                                getBidder(q).bid.spendLimit = getBidder(q).getAvgCost() / percUsedCapacity;
                            }
                        }
                    }
                } else {
                    for (BidOptimizer bo : bidders.values()) {
                        bo.bid.spendLimit = BidBundle.NO_SPEND_LIMIT;
                    }
                }
            }
        }
        // return bid
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
        needToSetBidLimits = true;
    }

    public void handleSalesReport(SalesReport salesReport) {
        for (Query q : agent.querySpace) {
            getBidder(q).handleSalesReport(salesReport);
        }
    }
}
