package eecs547.tac.aa.v1;

import eecs547.tac.aa.Bid;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
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

    public static final double CAPACITY_LIMIT_MIN = 0.5, TOP_EARNERS = 0.2;
    protected TigerBloodV1 agent;
    private HashMap<Query, BidOptimizer> bidders = new HashMap<Query, BidOptimizer>();
    private boolean needToSetBidLimits = true;

    public BidModel(TigerBloodV1 agent) {
        this.agent = agent;
    }

    public Bid getBid(Query query) {
        if (needToSetBidLimits) {
            needToSetBidLimits = false;
            if (getBidder(query).bid != null) {
                // optimize capacity
                double percUsedCapacity = (double) getUsedCapacity() / (double) agent.advertiserInfo.getDistributionCapacity();
                if (percUsedCapacity >= CAPACITY_LIMIT_MIN) {
                    // rank the bidders
                    List<BidOptimizer> rankBidders = new LinkedList<BidOptimizer>();
                    rankBidders.addAll(bidders.values());
                    Collections.sort(rankBidders, new Comparator<BidOptimizer>() {

                        public int compare(BidOptimizer bo1, BidOptimizer bo2) {
                            return -1 * Double.valueOf(bo1.getAvgCPConv()).compareTo(Double.valueOf(bo2.getAvgCPConv()));
                        }
                    });
                    // how many bidders to cut off?
                    double biddersToCutOff = Math.ceil((double) rankBidders.size() * (double) (percUsedCapacity - CAPACITY_LIMIT_MIN) * 2d);
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
