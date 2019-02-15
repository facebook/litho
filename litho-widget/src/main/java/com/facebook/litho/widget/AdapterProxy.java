package com.facebook.litho.widget;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * A proxy class for RecyclerBinder's internal adapter to directly call through to another
 * {@link RecyclerView.Adapter} implementation. Useful for low overhead integration with an existing
 * {@link RecyclerView} with mixed Litho and Android views.
 */
public interface AdapterProxy<H extends RecyclerView.ViewHolder>  {

    // Placeholder RenderInfo to signal RecyclerBinder to call directly to AdapterProxy.
    RenderInfo PROXY_RENDER_INFO = new BaseRenderInfo(new BaseRenderInfo.Builder() {}) {
        @Override
        public boolean rendersComponent() {
            return false;
        }

        @Override
        public boolean rendersView() {
            return true;
        }

        @Override
        public String getName() {
            return "AdapterProxyRenderInfo";
        }
    };

    H onCreateViewHolder(ViewGroup parent, int viewType);

    void onBindViewHolder(H holder, int position);

    int getItemViewType(int position);

    void onViewRecycled(H holder);
}
