package com.yokong.servlet;

import com.google.gson.Gson;
import com.yokong.data.DataProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class PicServlet extends HttpServlet {
    public PicServlet() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DataProvider provider = DataProvider.getInstance();
        response.setCharacterEncoding("UTF-8");
        response.setHeader("content-type", "application/json;charset=UTF-8");

        DataProvider.PhotoList list = provider.getPhotoList(0);
        PrintWriter out = null;
        Gson gson = new Gson();
        ResponseData responseData = new ResponseData();
        try {
            out = response.getWriter();
            if (list == null) {
                responseData.result = "403";
                out.write(gson.toJson(responseData));
                return;
            }
            responseData.result = "200";
            responseData.data = list;
            out.write(gson.toJson(responseData));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    class ResponseData {
        String result;
        DataProvider.PhotoList data;
    }

}
