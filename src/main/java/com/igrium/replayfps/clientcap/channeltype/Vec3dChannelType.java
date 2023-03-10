package com.igrium.replayfps.clientcap.channeltype;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class Vec3dChannelType implements ChannelType<Vector3dc> {

    @Override
    public int getLength() {
        return Double.BYTES * 3;
    }

    @Override
    public Vector3d read(DataInputStream in) throws IOException {
        return read(in, new Vector3d());
    }

    public Vector3d read(DataInputStream in, Vector3d dest) throws IOException {
        dest.x = in.readDouble();
        dest.y = in.readDouble();
        dest.z = in.readDouble();
        return dest;
    }

    @Override
    public void write(DataOutputStream out, Vector3dc val) throws IOException {
        out.writeDouble(val.x());
        out.writeDouble(val.y());
        out.writeDouble(val.z());
    }

    @Override
    public Class<? extends Vector3dc> getChannelClass() {
        return Vector3dc.class;
    }
    
    @Override
    public Vector3d lerp(Vector3dc from, Vector3dc to, float fac) {
        return from.lerp(to, fac, new Vector3d());
    }
}
