package se.simonevertsson.experiments;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import se.simonevertsson.db.DatabaseService;
import se.simonevertsson.gpu.*;
import se.simonevertsson.query.QueryGraph;
import se.simonevertsson.query.QueryGraphGenerator;

import java.io.IOException;

/**
 * Created by simon.evertsson on 2015-05-19.
 */
public class GpuQueryRunner {

    public void runGpuQuery(DatabaseService databaseService) throws IOException {
        long tick, tock;

        /* Convert database data and query data to fit the GPU */
        tick = System.currentTimeMillis();
        LabelDictionary labelDictionary = new LabelDictionary();
        QueryGraph queryGraph = QueryGraphGenerator.generateMockQueryGraph();

        GpuGraphModel gpuData = convertData(databaseService, labelDictionary);
        GpuGraphModel gpuQuery = convertQuery(labelDictionary, queryGraph);
        tock = System.currentTimeMillis();

        System.out.println("GPU Data conversion runtime: " + (tock-tick) + "ms");

        /* Execute the query */
        tick = System.currentTimeMillis();
        GpuQuery gpuGraphQuery = new GpuQuery();
        gpuGraphQuery.executeQuery(gpuData, gpuQuery, queryGraph);
        tock = System.currentTimeMillis();

        System.out.println("GPU Query runtime: " + (tock - tick) + "ms");
    }

    private GpuGraphModel convertQuery(LabelDictionary labelDictionary, QueryGraph queryGraph) {
        SpanningTreeGenerator.generateQueryGraph(queryGraph, labelDictionary);
        return GraphModelConverter.convertNodesToGpuGraphModel(queryGraph.visitOrder, labelDictionary);
    }

    private GpuGraphModel convertData(DatabaseService databaseService, LabelDictionary labelDictionary) {
        ResourceIterable<Node> allNodes = databaseService.getAllNodes();
        return GraphModelConverter.convertNodesToGpuGraphModel(allNodes, labelDictionary);
    }
}