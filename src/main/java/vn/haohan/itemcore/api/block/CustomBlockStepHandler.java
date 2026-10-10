package vn.haohan.itemcore.api.block;

@FunctionalInterface
public interface CustomBlockStepHandler {
    void onStep(CustomBlockStepContext context);
}
