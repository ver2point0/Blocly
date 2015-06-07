package io.bloc.android.blocly.api.model;

public abstract class Model {

    private final long mRowId;

    public Model(long rowId) {
        mRowId = rowId;
    }

    public long getRowId() {
        return mRowId;
    }
}
