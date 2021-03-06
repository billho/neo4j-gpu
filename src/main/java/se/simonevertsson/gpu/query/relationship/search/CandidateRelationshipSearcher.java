package se.simonevertsson.gpu.query.relationship.search;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLMem;
import org.bridj.Pointer;
import org.neo4j.graphdb.Relationship;
import se.simonevertsson.gpu.buffer.BufferContainer;
import se.simonevertsson.gpu.buffer.QueryBuffers;
import se.simonevertsson.gpu.kernel.QueryKernels;
import se.simonevertsson.gpu.query.QueryContext;
import se.simonevertsson.gpu.query.QueryUtils;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class CandidateRelationshipSearcher {

  private final QueryContext queryContext;
  private final QueryBuffers queryBuffers;
  private final int dataNodeCount;
  private final CandidateRelationshipCounter candidateRelationshipCounter;
  private final QueryKernels queryKernels;
  private final CandidateRelationshipFinder candidateRelationshipFinder;

  public CandidateRelationshipSearcher(QueryContext queryContext, QueryKernels queryKernels, BufferContainer bufferContainer) {
    this.queryBuffers = bufferContainer.queryBuffers;
    this.queryContext = queryContext;
    this.queryKernels = queryKernels;
    this.dataNodeCount = queryContext.dataNodeCount;
    this.candidateRelationshipCounter = new CandidateRelationshipCounter(queryKernels, bufferContainer, this.dataNodeCount);
    this.candidateRelationshipFinder = new CandidateRelationshipFinder(queryKernels, bufferContainer, this.dataNodeCount);
  }

  public HashMap<Integer, CandidateRelationships> searchCandidateRelationships() throws IOException {
    ArrayList<Relationship> relationships = this.queryContext.queryGraph.relationships;
    HashMap<Integer, CandidateRelationships> candidateRelationshipsHashMap = new HashMap<>();

    for (Relationship relationship : relationships) {
      CandidateRelationships candidateRelationships = new CandidateRelationships(relationship, this.queryContext, this.queryKernels);

      Pointer<Integer> candidateRelationshipCountsPointer = this.candidateRelationshipCounter.countCandidateRelationships(candidateRelationships);

      int[] candidateRelationshipEndNodeIndices = QueryUtils.generatePrefixScanArray(candidateRelationshipCountsPointer, candidateRelationships.getStartNodeCount());
      CLBuffer<Integer>
          candidateRelationshipEndNodeIndicesBuffer = this.queryKernels.context.createIntBuffer(CLMem.Usage.Input, IntBuffer.wrap(candidateRelationshipEndNodeIndices), true);

      candidateRelationships.setCandidateEndNodeIndices(candidateRelationshipEndNodeIndicesBuffer);
      candidateRelationships.setEndNodeCount(candidateRelationshipEndNodeIndices[candidateRelationshipEndNodeIndices.length - 1]);

      this.candidateRelationshipFinder.findCandidateRelationships(candidateRelationships);

      candidateRelationshipsHashMap.put((int) relationship.getId(), candidateRelationships);

    }

    return candidateRelationshipsHashMap;
  }

}