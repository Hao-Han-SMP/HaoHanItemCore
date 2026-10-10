package vn.haohan.itemcore.api.block;

@FunctionalInterface
public interface CustomBlockInteractHandler {
    void onInteract(CustomBlockInteractContext context);
}
