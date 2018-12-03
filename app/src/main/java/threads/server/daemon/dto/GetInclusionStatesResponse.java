package threads.server.daemon.dto;

public class GetInclusionStatesResponse extends AbstractResponse {

    private boolean[] states;

    public static AbstractResponse create(boolean[] inclusionStates) {
        GetInclusionStatesResponse res = new GetInclusionStatesResponse();
        res.states = inclusionStates;
        return res;
    }

    public boolean[] getStates() {
        return states;
    }

}
