package threads.server;


import threads.core.IAesKey;

public class AesKey implements IAesKey {


    @Override
    public String getAesKey() {
        return threads.server.Application.APPLICATION_AES_KEY;
    }

}
