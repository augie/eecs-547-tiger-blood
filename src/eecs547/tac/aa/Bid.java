package eecs547.tac.aa;

import edu.umich.eecs.tac.props.Ad;

/**
 *
 * @author Augie
 */
public class Bid {

    public double bid, spendLimit;
    public Ad ad;

    public Bid(double bid, Ad ad, double spendLimit) {
        this.bid = bid;
        this.ad = ad;
        this.spendLimit = spendLimit;
    }
}
