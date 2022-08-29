package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public final class RootComponent extends Component {

    @Override
    protected RenderResult render(ComponentContext c, int widthSpec, int heightSpec) {
        return new RenderResult(null);
    }
}