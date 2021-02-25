package juddy.transport.api;

import juddy.transport.api.common.ApiBean;

import java.util.List;

@ApiBean(TestApiPhaseTwo.class)
public class TestApiPhaseTwoServer implements TestApiPhaseTwo {

    @Override
    public int size(List<String> cities) {
        return cities.size();
    }
}
