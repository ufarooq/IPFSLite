package threads.server.daemon.dto;

public class AddedNeighborsResponse extends AbstractResponse {

    private int addedNeighbors;

    public static AbstractResponse create(int numberOfAddedNeighbors) {
        AddedNeighborsResponse res = new AddedNeighborsResponse();
        res.addedNeighbors = numberOfAddedNeighbors;
        return res;
    }

    public int getAddedNeighbors() {
        return addedNeighbors;
    }

}
