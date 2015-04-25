package eecs547.tac.aa.naive;

import eecs547.tac.aa.Util;
import eecs547.tac.aa.Bid;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import java.util.LinkedList;

/**
 *
 * @author Augie
 */
public class BidOptimizer {

    public static final double MOVEMENT = 0.05d, PERCENT_INITIAL_BID_F0 = 0.04, PERCENT_INITIAL_BID_F1 = 0.06, PERCENT_INITIAL_BID_F2 = 0.1;
    public BidModel bM;
    public Query query;
    public Bid bid;
    private LinkedList<Double> bids = new LinkedList<Double>(), costs = new LinkedList<Double>(), revenues = new LinkedList<Double>(), cpc = new LinkedList<Double>(), position = new LinkedList<Double>();
    private LinkedList<Integer> clicks = new LinkedList<Integer>(), conversions = new LinkedList<Integer>();
    public Double bidDelta = 0d;
    private boolean firstBid = true;

    public BidOptimizer(BidModel bM, Query query) {
        this.bM = bM;
        this.query = query;
    }

    public Bid getBid() {
        updateBid();
        bids.add(bid.bid);
        return bid;
    }

    public void updateBid() {
        // set random on first update
        if (firstBid) {
            bid = new Bid(BidBundle.NO_SHOW_BID, new Ad(null), BidBundle.NO_SPEND_LIMIT);

            // set a random bid with mean at half of the profit
            double avgProfit = bM.agent.getAverageSalesProfit(query.getComponent(), query.getManufacturer());
            if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                bid.bid = avgProfit * PERCENT_INITIAL_BID_F0;
            } else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                bid.bid = avgProfit * PERCENT_INITIAL_BID_F1;
            } else if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
                bid.bid = avgProfit * PERCENT_INITIAL_BID_F2;
            }

            // start bid delta
            bidDelta = MOVEMENT * bid.bid;

            firstBid = false;
        }

        // update direction
        if ((!revenues.isEmpty() && getProfit(0) < 0 && bidDelta > 0)
                || (bid.bid <= 0 && bidDelta < 0)
                || (revenues.size() >= 2 && getProfit(1) > getProfit(0))) {
            bidDelta *= -1;
        }

        // submit the bid
        bid.bid = Util.round3(bid.bid + bidDelta);
        if (bid.bid < 0) {
            bid.bid = 0;
        }
    }

    public void handleQueryReport(QueryReport queryReport) {
        setTotalCost(queryReport.getCost(query));
        setClicks(queryReport.getClicks(query));
        setCPC(queryReport.getCPC(query));
        setPosition(queryReport.getPosition(query));
    }

    public void handleSalesReport(SalesReport salesReport) {
        setRevenue(salesReport.getRevenue(query));
        setConversions(salesReport.getConversions(query));
    }

    public int getClicks() {
        return clicks.getFirst();
    }

    public int getConversions() {
        return getConversions(0);
    }

    public int getConversions(int i) {
        if (conversions.isEmpty() || i >= conversions.size()) {
            return 0;
        }
        return conversions.get(i);
    }

    public double getCPC() {
        return cpc.getFirst();
    }

    public double getCPConv() {
        if (conversions.isEmpty() || conversions.getFirst().equals(Double.NaN) || conversions.getFirst() == 0) {
            return 0d;
        }
        return costs.getFirst() / conversions.getFirst();
    }

    public double getPosition() {
        if (position.isEmpty() || position.getFirst().equals(Double.NaN) || position.getFirst() == 0) {
            return 0;
        }
        return 9d - position.getFirst();
    }

    public Double getProfit(int i) {
        if (i > revenues.size() - 1) {
            return null;
        }
        return revenues.get(i) - costs.get(i);
    }

    public double getRevenue() {
        if (revenues.isEmpty()) {
            return 0;
        }
        return revenues.getFirst();
    }

    public void setTotalCost(double totalCost) {
        if (bids.size() < 2) {
            return;
        }
        costs.addFirst(totalCost);
    }

    public void setClicks(int clicks) {
        this.clicks.addFirst(clicks);
    }

    public void setConversions(int conversions) {
        this.conversions.addFirst(conversions);
    }

    public void setCPC(double cpc) {
        this.cpc.addFirst(cpc);
    }

    public void setPosition(double position) {
        this.position.addFirst(position);
    }

    public void setRevenue(double revenue) {
        if (bids.size() < 2) {
            return;
        }
        revenues.addFirst(revenue);
    }
}
