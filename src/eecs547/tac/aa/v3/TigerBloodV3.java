package eecs547.tac.aa.v3;

import eecs547.tac.aa.Util;
import eecs547.tac.aa.Bid;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import eecs547.tac.aa.BlankModel;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;

/**
 *
 * @author Augie
 */
public class TigerBloodV3 extends Agent {

    public static final String NAME = "V3";
    /**
     * Basic simulation information. {@link StartInfo} contains
     * <ul>
     * <li>simulation ID</li>
     * <li>simulation start time</li>
     * <li>simulation length in simulation days</li>
     * <li>actual seconds per simulation day</li>
     * </ul>
     * An agent should receive the {@link StartInfo} at the beginning of the game or during recovery.
     */
    private StartInfo startInfo;
    /**
     * Basic auction slot information. {@link SlotInfo} contains
     * <ul>
     * <li>the number of regular slots</li>
     * <li>the number of promoted slots</li>
     * <li>promoted slot bonus</li>
     * </ul>
     * An agent should receive the {@link SlotInfo} at the beginning of the game or during recovery.
     * This information is identical for all auctions over all query classes.
     */
    protected SlotInfo slotInfo;
    /**
     * The retail catalog. {@link RetailCatalog} contains
     * <ul>
     * <li>the product set</li>
     * <li>the sales profit per product</li>
     * <li>the manufacturer set</li>
     * <li>the component set</li>
     * </ul>
     * An agent should receive the {@link RetailCatalog} at the beginning of the game or during recovery.
     */
    protected RetailCatalog retailCatalog;
    /**
     * The basic advertiser specific information. {@link AdvertiserInfo} contains
     * <ul>
     * <li>the manufacturer specialty</li>
     * <li>the component specialty</li>
     * <li>the manufacturer bonus</li>
     * <li>the component bonus</li>
     * <li>the distribution capacity discounter</li>
     * <li>the address of the publisher agent</li>
     * <li>the distribution capacity</li>
     * <li>the address of the advertiser agent</li>
     * <li>the distribution window</li>
     * <li>the target effect</li>
     * <li>the focus effects</li>
     * </ul>
     * An agent should receive the {@link AdvertiserInfo} at the beginning of the game or during recovery.
     */
    protected AdvertiserInfo advertiserInfo;
    /**
     * The basic publisher information. {@link PublisherInfo} contains
     * <ul>
     * <li>the squashing parameter</li>
     * </ul>
     * An agent should receive the {@link PublisherInfo} at the beginning of the game or during recovery.
     */
    protected PublisherInfo publisherInfo;
    /**
     * The list contains all of the {@link SalesReport sales report} delivered to the agent.  Each
     * {@link SalesReport sales report} contains the conversions and sales revenue accrued by the agent for each query
     * class during the period.
     */
    protected Queue<SalesReport> salesReports = new LinkedList<SalesReport>();
    /**
     * The list contains all of the {@link QueryReport query reports} delivered to the agent.  Each
     * {@link QueryReport query report} contains the impressions, clicks, cost, average position, and ad displayed
     * by the agent for each query class during the period as well as the positions and displayed ads of all advertisers
     * during the period for each query class.
     */
    protected Queue<QueryReport> queryReports = new LinkedList<QueryReport>();
    /**
     * List of all the possible queries made available in the {@link RetailCatalog retail catalog}.
     */
    protected Set<Query> querySpace = new LinkedHashSet<Query>();
    /**
     * Models the game opponents.
     */
    protected GameModel gM;
    /**
     * Models the this agent relative to the game opponents
     */
    protected BidModel bM;
    /**
     * GUI tools
     */
    private BlankModel blankModel;
    private OpenSequenceGraph surplusGraph, revenueGraph, bidGraph, impressionGraph, conversionGraph, clickGraph, cpcGraph, cpConvGraph, positionGraph, usedCapacityGraph;
    /**
     * Remmeber the special query.
     */
    private Query specialQuery;

    /*
     * Returns the average sales profit over all products
     */
    public double getAverageSalesProfit(String component, String manufacturer) {
        double t = 0d, c = 0d;
        for (Product p : retailCatalog.keys()) {
            if ((component == null || component.equals(p.getComponent())) && (manufacturer == null || manufacturer.equals(p.getManufacturer()))) {
                t += retailCatalog.getSalesProfit(p);
                c++;
            }
        }
        return t / c;
    }

    protected Query getSpecializationQuery() {
        if (specialQuery == null) {
            for (Query q : querySpace) {
                try {
                    if (q.getManufacturer() != null && q.getManufacturer().equals(advertiserInfo.getManufacturerSpecialty()) && q.getComponent() != null && q.getComponent().equals(advertiserInfo.getComponentSpecialty())) {
                        specialQuery = q;
                        break;
                    }
                } catch (Exception e) {
                }
            }
        }
        return specialQuery;
    }

    /**
     * Processes the messages received the by agent from the server.
     *
     * @param message the message
     */
    protected void messageReceived(Message message) {
        Transportable content = message.getContent();
        if (content instanceof QueryReport) {
            handleQueryReport((QueryReport) content);
        } else if (content instanceof SalesReport) {
            handleSalesReport((SalesReport) content);
        } else if (content instanceof SimulationStatus) {
            handleSimulationStatus((SimulationStatus) content);
        } else if (content instanceof PublisherInfo) {
            handlePublisherInfo((PublisherInfo) content);
        } else if (content instanceof SlotInfo) {
            handleSlotInfo((SlotInfo) content);
        } else if (content instanceof RetailCatalog) {
            handleRetailCatalog((RetailCatalog) content);
        } else if (content instanceof AdvertiserInfo) {
            handleAdvertiserInfo((AdvertiserInfo) content);
        } else if (content instanceof StartInfo) {
            handleStartInfo((StartInfo) content);
        }
    }

    /**
     * Sends a constructed {@link BidBundle} from any updated bids, ads, or spend limits.
     */
    protected void sendBidAndAds() {
        try {
            BidBundle bidBundle = new BidBundle();
            String publisherAddress = advertiserInfo.getPublisherId();
            for (Query query : querySpace) {
                Bid b = bM.getBid(query);
                bidBundle.addQuery(query, b.bid, b.ad);
                bidBundle.setDailyLimit(query, b.spendLimit);
            }
            bidBundle.setCampaignDailySpendLimit(bM.getDailySpendLimit());

            // Send the bid bundle to the publisher
            if (publisherAddress != null) {
                sendMessage(publisherAddress, bidBundle);
            }
        } catch (Exception e) {
            Util.debug(e);
        }
        bidGraph.step();
    }

    /**
     * Processes an incoming query report.
     *
     * @param queryReport the daily query report.
     */
    protected void handleQueryReport(QueryReport queryReport) {
        queryReports.add(queryReport);
        gM.handleQueryReport(queryReport);
        bM.handleQueryReport(queryReport);
        impressionGraph.step();
        clickGraph.step();
        cpcGraph.step();
        positionGraph.step();
    }

    /**
     * Processes an incoming sales report.
     *
     * @param salesReport the daily sales report.
     */
    protected void handleSalesReport(SalesReport salesReport) {
        salesReports.add(salesReport);
        bM.handleSalesReport(salesReport);
        revenueGraph.step();
        conversionGraph.step();
        surplusGraph.step();
        cpConvGraph.step();
        usedCapacityGraph.step();
    }

    /**
     * Processes a simulation status notification.  Each simulation day the {@link SimulationStatus simulation status }
     * notification is sent after the other daily messages ({@link QueryReport} {@link SalesReport} have been sent.
     *
     * @param simulationStatus the daily simulation status.
     */
    protected void handleSimulationStatus(SimulationStatus simulationStatus) {
        sendBidAndAds();
    }

    /**
     * Processes the publisher information.
     * @param publisherInfo the publisher information.
     */
    protected void handlePublisherInfo(PublisherInfo publisherInfo) {
        this.publisherInfo = publisherInfo;
    }

    /**
     * Processrs the slot information.
     * @param slotInfo the slot information.
     */
    protected void handleSlotInfo(SlotInfo slotInfo) {
        this.slotInfo = slotInfo;
    }

    /**
     * Processes the retail catalog.
     * @param retailCatalog the retail catalog.
     */
    protected void handleRetailCatalog(RetailCatalog retailCatalog) {
        this.retailCatalog = retailCatalog;

        // The query space is all the F0, F1, and F2 queries for each product
        // The F0 query class
        if (retailCatalog.size() > 0) {
            querySpace.add(new Query(null, null));
        }

        for (Product product : retailCatalog) {
            // F1 Manufacturer only
            querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            querySpace.add(new Query(null, product.getComponent()));
            // The F2 query class
            querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
        }

        // set up the queries
        for (final Query q : querySpace) {
            surplusGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getProfit(0);
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            revenueGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    return bM.getBidder(q).getRevenue();
                }
            });
            bidGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).bid.bid;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            impressionGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return gM.uM.getImpressions(q).getFirst();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            clickGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getClicks();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            conversionGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getConversions();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            cpcGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getCPC();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            cpConvGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getCPConv();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            positionGraph.addSequence(q.getComponent() + ", " + q.getManufacturer(), new Sequence() {

                public double getSValue() {
                    try {
                        return bM.getBidder(q).getPosition();
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
        }
        usedCapacityGraph.addSequence("Used Capacity", new Sequence() {

            public double getSValue() {
                try {
                    return bM.getUsedCapacity();
                } catch (Exception e) {
                    return 0;
                }
            }
        });
        surplusGraph.display();
        revenueGraph.display();
        bidGraph.display();
        impressionGraph.display();
        clickGraph.display();
        conversionGraph.display();
        cpcGraph.display();
        cpConvGraph.display();
        positionGraph.display();
        usedCapacityGraph.display();
        blankModel.run();
    }

    /**
     * Processes the advertiser information.
     * @param advertiserInfo the advertiser information.
     */
    protected void handleAdvertiserInfo(AdvertiserInfo advertiserInfo) {
        this.advertiserInfo = advertiserInfo;
        Util.debug("Capacity: " + advertiserInfo.getDistributionCapacity() + ", Distribution Window: " + advertiserInfo.getDistributionWindow() + ", Distribution Capacity Discounter: " + advertiserInfo.getDistributionCapacityDiscounter());
    }

    /**
     * Processes the start information.
     * @param startInfo the start information.
     */
    protected void handleStartInfo(StartInfo startInfo) {
        this.startInfo = startInfo;
    }

    /**
     * Prepares the agent for a new simulation.
     */
    protected void simulationSetup() {
        gM = new GameModel(this);
        bM = new BidModel(this);
        blankModel = new BlankModel();

        if (surplusGraph != null) {
            surplusGraph.dispose();
            surplusGraph = null;
        }
        surplusGraph = new OpenSequenceGraph(NAME + " Surplus", blankModel);
        surplusGraph.setXRange(0, 60);
        surplusGraph.setYRange(0, 1000);
        surplusGraph.setYIncrement(100);
        surplusGraph.setAxisTitles("Time", "$");

        if (revenueGraph != null) {
            revenueGraph.dispose();
        }
        revenueGraph = new OpenSequenceGraph(NAME + " Revenue", blankModel);
        revenueGraph.setXRange(0, 60);
        revenueGraph.setYRange(0, 1000);
        revenueGraph.setYIncrement(100);
        revenueGraph.setAxisTitles("Time", "$");

        if (bidGraph != null) {
            bidGraph.dispose();
        }
        bidGraph = new OpenSequenceGraph(NAME + " Bids", blankModel);
        bidGraph.setXRange(0, 60);
        bidGraph.setYRange(0, 2);
        bidGraph.setYIncrement(0.2);
        bidGraph.setAxisTitles("Time", "$");

        if (impressionGraph != null) {
            impressionGraph.dispose();
        }
        impressionGraph = new OpenSequenceGraph(NAME + " Impressions", blankModel);
        impressionGraph.setXRange(0, 60);
        impressionGraph.setYRange(0, 500);
        impressionGraph.setYIncrement(50);
        impressionGraph.setAxisTitles("Time", "Impressions");

        if (clickGraph != null) {
            clickGraph.dispose();
        }
        clickGraph = new OpenSequenceGraph(NAME + " Clicks", blankModel);
        clickGraph.setXRange(0, 60);
        clickGraph.setYRange(0, 200);
        clickGraph.setYIncrement(20);
        clickGraph.setAxisTitles("Time", "Clicks");

        if (conversionGraph != null) {
            conversionGraph.dispose();
        }
        conversionGraph = new OpenSequenceGraph(NAME + " Conversions", blankModel);
        conversionGraph.setXRange(0, 60);
        conversionGraph.setYRange(0, 100);
        conversionGraph.setYIncrement(10);
        conversionGraph.setAxisTitles("Time", "Conversions");

        if (cpcGraph != null) {
            cpcGraph.dispose();
        }
        cpcGraph = new OpenSequenceGraph(NAME + " Cost Per Click", blankModel);
        cpcGraph.setXRange(0, 60);
        cpcGraph.setYRange(0, 2);
        cpcGraph.setYIncrement(0.2);
        cpcGraph.setAxisTitles("Time", "$");

        if (cpConvGraph != null) {
            cpConvGraph.dispose();
        }
        cpConvGraph = new OpenSequenceGraph(NAME + " Cost Per Conversion", blankModel);
        cpConvGraph.setXRange(0, 60);
        cpConvGraph.setYRange(0, 10);
        cpConvGraph.setYIncrement(1);
        cpConvGraph.setAxisTitles("Time", "$");

        if (positionGraph != null) {
            positionGraph.dispose();
        }
        positionGraph = new OpenSequenceGraph(NAME + " Position", blankModel);
        positionGraph.setXRange(0, 60);
        positionGraph.setYRange(0, 8);
        positionGraph.setYIncrement(1);
        positionGraph.setAxisTitles("Time", "Position");

        if (usedCapacityGraph != null) {
            usedCapacityGraph.dispose();
        }
        usedCapacityGraph = new OpenSequenceGraph(NAME + " Used Capacity", blankModel);
        usedCapacityGraph.setXRange(0, 60);
        usedCapacityGraph.setYRange(0, 600);
        usedCapacityGraph.setYIncrement(50);
        usedCapacityGraph.setAxisTitles("Time", "Units");
    }

    /**
     * Runs any post-processes required for the agent after a simulation ends.
     */
    protected void simulationFinished() {
        salesReports.clear();
        queryReports.clear();
        querySpace.clear();
        // set graph file names
        surplusGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_surplus");
        revenueGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_revenue");
        bidGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_bid");
        impressionGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_impression");
        conversionGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_conversion");
        clickGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_click");
        cpcGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_cpc");
        cpConvGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_cpConv");
        positionGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_position");
        usedCapacityGraph.setSnapshotFileName("../tac-aa-11/game-graphs/" + startInfo.getSimulationID() + "_" + NAME + "_usedCapacity");
        // save the graphs
        surplusGraph.takeSnapshot();
        revenueGraph.takeSnapshot();
        bidGraph.takeSnapshot();
        impressionGraph.takeSnapshot();
        conversionGraph.takeSnapshot();
        clickGraph.takeSnapshot();
        cpcGraph.takeSnapshot();
        cpConvGraph.takeSnapshot();
        positionGraph.takeSnapshot();
        usedCapacityGraph.takeSnapshot();
        // close it
        surplusGraph.closeMovie();
        revenueGraph.closeMovie();
        bidGraph.closeMovie();
        impressionGraph.closeMovie();
        conversionGraph.closeMovie();
        clickGraph.closeMovie();
        cpcGraph.closeMovie();
        cpConvGraph.closeMovie();
        positionGraph.closeMovie();
        usedCapacityGraph.closeMovie();
        // dispose of the graphs
        surplusGraph.dispose();
        revenueGraph.dispose();
        bidGraph.dispose();
        impressionGraph.dispose();
        conversionGraph.dispose();
        clickGraph.dispose();
        cpcGraph.dispose();
        cpConvGraph.dispose();
        positionGraph.dispose();
        usedCapacityGraph.dispose();
        // set graphs to null
        surplusGraph = null;
        revenueGraph = null;
        bidGraph = null;
        impressionGraph = null;
        conversionGraph = null;
        clickGraph = null;
        cpcGraph = null;
        cpConvGraph = null;
        positionGraph = null;
        usedCapacityGraph = null;
        // stop the model
        blankModel.stop();
        blankModel = null;
        // force gc
        System.gc();
    }
}
