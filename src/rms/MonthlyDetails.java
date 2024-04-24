/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package rms;

import java.awt.Color;
import java.sql.SQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author hm
 */
public class MonthlyDetails extends javax.swing.JFrame {

    DefaultTableModel model;
    List<Integer> modifiedRows = new ArrayList<>();

    /**
     * Creates new form MonthlyDetails
     */
    public MonthlyDetails() {
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        tbl_data.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && e.getColumn() != -1) {
                    int rowIndex = e.getFirstRow();
                    if (!modifiedRows.contains(rowIndex)) {
                        modifiedRows.add(rowIndex);
                    }
                }
            }
        });

//        String[] columnNames = {"Paid/Unpaid", "Name", "Room No", "Monthly Rent", "Phone No", "Start Date"};
//        model = new DefaultTableModel(columnNames, 0);
//        tbl_data.setModel(model);
//
//        // Populate the JComboBox with data from the database
//        JComboBox<String> comboBox = new JComboBox<>();
//        populateComboBox(comboBox);
//
//        TableColumn column = tbl_data.getColumnModel().getColumn(0);
//        column.setCellEditor(new DefaultCellEditor(comboBox));
//
//        JScrollPane scrollPane = new JScrollPane(tbl_data);
//        jPanel1.setLayout(new GridLayout());
//        jPanel1.add(scrollPane);
    }

    private void populateComboBox(JComboBox<String> comboBox) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = DbConnection.getConnection();
            String query = "SELECT DISTINCT status_paid_unpaid FROM month_details"; // Modify the query as per your database schema
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status_paid_unpaid");
                comboBox.addItem(status);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setRecordToTable(String monthToCheck) {
        Connection con = DbConnection.getConnection();
        PreparedStatement pstCheck = null;
        PreparedStatement pstSelect = null;
        PreparedStatement pstInsert = null;
        PreparedStatement pstFetchTenants = null;
        DefaultTableModel model = (DefaultTableModel) tbl_data.getModel();

        try {
            con.setAutoCommit(false);

            pstCheck = con.prepareStatement("SELECT COUNT(*) FROM month_details WHERE month = ?");
            pstCheck.setString(1, monthToCheck);
            ResultSet rs = pstCheck.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                pstSelect = con.prepareStatement("SELECT tenant.name, tenant.phone_no, tenant.room_no, tenant.monthly_rent,tenant.start_date, "
                        + "month_details.status_paid_unpaid "
                        + "FROM tenant "
                        + "LEFT JOIN month_details ON tenant.room_no = month_details.room_no AND month_details.month = ? "
                        + "WHERE tenant.status = 'Available'");
                pstSelect.setString(1, monthToCheck);
                rs = pstSelect.executeQuery();
                model.setRowCount(0);

                while (rs.next()) {
                    String name = rs.getString("name");
                    String phoneNo = rs.getString("phone_no");
                    String roomNo = rs.getString("room_no");
                    double monthlyRent = rs.getDouble("monthly_rent");
                    String paymentStatus = rs.getString("status_paid_unpaid");
                    String startDate = rs.getString("start_date");
                    Object[] rowData = {paymentStatus, name, roomNo, monthlyRent, phoneNo, startDate};
                    model.addRow(rowData);
                }
            } else {
                pstFetchTenants = con.prepareStatement("SELECT name, phone_no, room_no, monthly_rent,start_date FROM tenant WHERE status = 'Available'");
                rs = pstFetchTenants.executeQuery();

                pstInsert = con.prepareStatement("INSERT INTO month_details (room_no, month, rent, status_paid_unpaid) VALUES (?, ?, ?, ?)");

                while (rs.next()) {
                    String name = rs.getString("name");
                    String phoneNo = rs.getString("phone_no");
                    String roomNo = rs.getString("room_no");
                    double monthlyRent = rs.getDouble("monthly_rent");
                    String statusPaidUnpaid = "Unpaid";

                    pstInsert.setString(1, roomNo);
                    pstInsert.setString(2, monthToCheck);
                    pstInsert.setDouble(3, monthlyRent);
                    pstInsert.setString(4, statusPaidUnpaid);
                    pstInsert.addBatch();
                }

                int[] insertedRows = pstInsert.executeBatch();

                pstSelect = con.prepareStatement("SELECT tenant.name, tenant.phone_no, tenant.room_no, tenant.monthly_rent,start_date, "
                        + "month_details.status_paid_unpaid "
                        + "FROM tenant "
                        + "LEFT JOIN month_details ON tenant.room_no = month_details.room_no AND month_details.month = ? "
                        + "WHERE tenant.status = 'Available'");
                pstSelect.setString(1, monthToCheck);
                rs = pstSelect.executeQuery();
                model.setRowCount(0);

                while (rs.next()) {
                    String name = rs.getString("name");
                    String phoneNo = rs.getString("phone_no");
                    String roomNo = rs.getString("room_no");
                    double monthlyRent = rs.getDouble("monthly_rent");
                    String paymentStatus = rs.getString("status_paid_unpaid");
                    String startDate = rs.getString("start_date");
                    Object[] rowData = {paymentStatus, name, roomNo, monthlyRent, phoneNo, startDate};
                    model.addRow(rowData);
                }
            }

            con.commit();
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (pstCheck != null) {
                    pstCheck.close();
                }
                if (pstSelect != null) {
                    pstSelect.close();
                }
                if (pstInsert != null) {
                    pstInsert.close();
                }
                if (pstFetchTenants != null) {
                    pstFetchTenants.close();
                }
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_data = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        txtmonth = new com.toedter.calendar.JMonthChooser();
        btnSubmit = new javax.swing.JButton();
        btnSearch = new javax.swing.JButton();
        jYearChooser1 = new com.toedter.calendar.JYearChooser();
        jLabel3 = new javax.swing.JLabel();
        txtTotalAmount = new javax.swing.JTextField();
        panelSideBar = new javax.swing.JPanel();
        panelSearchRecord = new javax.swing.JPanel();
        btnAllTenant = new javax.swing.JLabel();
        panelHome = new javax.swing.JPanel();
        btnHome = new javax.swing.JLabel();
        panelEditc = new javax.swing.JPanel();
        btnEditTenant = new javax.swing.JLabel();
        panelCourseL = new javax.swing.JPanel();
        btnMonthlyRecords = new javax.swing.JLabel();
        panelBack = new javax.swing.JPanel();
        btnMonthlyRent = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(245, 232, 221));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 0));
        jLabel1.setText("Rent Management System");
        jPanel2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 0, 280, 60));

        jLabel9.setFont(new java.awt.Font("SimSun-ExtB", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(0, 0, 0));
        jLabel9.setText("Month payment");
        jPanel2.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 60, 160, 30));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(286, 0, 1080, 100));

        jPanel1.setBackground(new java.awt.Color(204, 211, 202));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        tbl_data.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Paid/Unpaid", "Name", "Room No", "Monthly Rent", "Phone No", "Start Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tbl_data);
        if (tbl_data.getColumnModel().getColumnCount() > 0) {
            tbl_data.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(new String[]{"Paid", "Unpaid"}))
            );
        }

        jScrollPane2.setViewportView(jScrollPane1);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 100, 910, 370));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 0, 0));
        jLabel2.setText("Total Amount Received :");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 500, 200, 40));

        txtmonth.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jPanel1.add(txtmonth, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 40, -1, 40));

        btnSubmit.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnSubmit.setText("Submit");
        btnSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitActionPerformed(evt);
            }
        });
        jPanel1.add(btnSubmit, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 500, 120, 40));

        btnSearch.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        jPanel1.add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 40, 120, 40));

        jYearChooser1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jPanel1.add(jYearChooser1, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 40, 120, 40));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 0, 0));
        jLabel3.setText("Month :");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 40, 70, 40));

        txtTotalAmount.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        txtTotalAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalAmountActionPerformed(evt);
            }
        });
        jPanel1.add(txtTotalAmount, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 500, 250, 40));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(286, 98, 1080, 600));

        panelSideBar.setBackground(new java.awt.Color(238, 211, 217));
        panelSideBar.setPreferredSize(new java.awt.Dimension(370, 740));
        panelSideBar.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelSearchRecord.setBackground(new java.awt.Color(218, 163, 175));
        panelSearchRecord.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
        panelSearchRecord.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        panelSearchRecord.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnAllTenant.setFont(new java.awt.Font("Sylfaen", 0, 24)); // NOI18N
        btnAllTenant.setForeground(new java.awt.Color(255, 255, 255));
        btnAllTenant.setText("All Tenant");
        btnAllTenant.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnAllTenantMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAllTenantMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAllTenantMouseExited(evt);
            }
        });
        panelSearchRecord.add(btnAllTenant, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 200, 50));

        panelSideBar.add(panelSearchRecord, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 210, 210, 70));

        panelHome.setBackground(new java.awt.Color(218, 163, 175));
        panelHome.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
        panelHome.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        panelHome.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnHome.setFont(new java.awt.Font("Sylfaen", 0, 24)); // NOI18N
        btnHome.setForeground(new java.awt.Color(255, 255, 255));
        btnHome.setText("Home");
        btnHome.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnHomeMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnHomeMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnHomeMouseExited(evt);
            }
        });
        panelHome.add(btnHome, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 200, 50));

        panelSideBar.add(panelHome, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 10, 210, 70));

        panelEditc.setBackground(new java.awt.Color(218, 163, 175));
        panelEditc.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
        panelEditc.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        panelEditc.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnEditTenant.setFont(new java.awt.Font("Sylfaen", 0, 24)); // NOI18N
        btnEditTenant.setForeground(new java.awt.Color(255, 255, 255));
        btnEditTenant.setText("Add Tenant");
        btnEditTenant.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnEditTenantMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEditTenantMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEditTenantMouseExited(evt);
            }
        });
        panelEditc.add(btnEditTenant, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 200, 50));

        panelSideBar.add(panelEditc, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 110, 210, 70));

        panelCourseL.setBackground(new java.awt.Color(218, 163, 175));
        panelCourseL.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
        panelCourseL.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        panelCourseL.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnMonthlyRecords.setFont(new java.awt.Font("Sylfaen", 0, 24)); // NOI18N
        btnMonthlyRecords.setForeground(new java.awt.Color(255, 255, 255));
        btnMonthlyRecords.setText("Back");
        btnMonthlyRecords.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnMonthlyRecordsMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnMonthlyRecordsMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnMonthlyRecordsMouseExited(evt);
            }
        });
        panelCourseL.add(btnMonthlyRecords, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 200, 50));

        panelSideBar.add(panelCourseL, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 410, 210, 70));

        panelBack.setBackground(new java.awt.Color(218, 163, 175));
        panelBack.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
        panelBack.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        panelBack.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnMonthlyRent.setFont(new java.awt.Font("Sylfaen", 0, 24)); // NOI18N
        btnMonthlyRent.setForeground(new java.awt.Color(255, 255, 255));
        btnMonthlyRent.setText(" Monthly Rent");
        btnMonthlyRent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnMonthlyRentMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnMonthlyRentMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnMonthlyRentMouseExited(evt);
            }
        });
        panelBack.add(btnMonthlyRent, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 200, 50));

        panelSideBar.add(panelBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 310, 210, 70));

        getContentPane().add(panelSideBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 290, 700));

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void btnSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitActionPerformed

        Connection con = DbConnection.getConnection();
        PreparedStatement pst = null;
        DefaultTableModel model = (DefaultTableModel) tbl_data.getModel();

        try {
            int selectedMonth = txtmonth.getMonth() + 1;
            int selectedYear = jYearChooser1.getYear();
            String month = String.format("%02d-%d", selectedMonth, selectedYear);

            con.setAutoCommit(false);

            for (int rowIndex : modifiedRows) {
                String roomNo = model.getValueAt(rowIndex, 2).toString();
                String statusPaidUnpaid = model.getValueAt(rowIndex, 0).toString();

                pst = con.prepareStatement("SELECT COUNT(*) FROM month_details WHERE room_no = ? AND month = ?");
                pst.setString(1, roomNo);
                pst.setString(2, month);
                ResultSet rs = pst.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    // Data already exists, update it
                    pst = con.prepareStatement("UPDATE month_details SET status_paid_unpaid = ? WHERE room_no = ? AND month = ?");
                    pst.setString(1, statusPaidUnpaid);
                    pst.setString(2, roomNo);
                    pst.setString(3, month);
                    pst.executeUpdate();
                } else {
                    // Data doesn't exist, insert it
                    pst = con.prepareStatement("INSERT INTO month_details (room_no, month, rent, status_paid_unpaid) VALUES (?, ?, ?, ?)");
                    pst.setString(1, roomNo);
                    pst.setString(2, month);
                    pst.setDouble(3, Double.parseDouble(model.getValueAt(rowIndex, 3).toString()));
                    pst.setString(4, statusPaidUnpaid);
                    pst.executeUpdate();
                }
            }

            con.commit();
            JOptionPane.showMessageDialog(this, "Modified rows updated successfully.");
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while updating modified rows.");
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        modifiedRows.clear();

    }//GEN-LAST:event_btnSubmitActionPerformed

    public void totalAmountReceived() {
        Connection con = DbConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        double totalAmount = 0.0;

        try {
            int selectedMonth = txtmonth.getMonth() + 1;
            int selectedYear = jYearChooser1.getYear();
            String month = String.format("%02d-%d", selectedMonth, selectedYear);

            con.setAutoCommit(false);

            pst = con.prepareStatement("SELECT SUM(rent) AS total_rent FROM month_details WHERE month = ? AND status_paid_unpaid = 'paid'");
            pst.setString(1, month);
            rs = pst.executeQuery();
            if (rs.next()) {
                totalAmount = rs.getDouble("total_rent");
            }

            con.commit();
        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        // Display or use the total amount (assuming you have a text field called txtTotalAmount)
        txtTotalAmount.setText(String.valueOf(totalAmount)); // If using GUI, set the total amount to the text field
    }

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed

        int selectedMonth = txtmonth.getMonth() + 1;
        int selectedYear = jYearChooser1.getYear();

        String monthToCheck = String.format("%02d-%d", selectedMonth, selectedYear);

        setRecordToTable(monthToCheck);
        totalAmountReceived();


    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnAllTenantMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAllTenantMouseClicked
        AllTenant allTenant = new AllTenant();
        allTenant.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnAllTenantMouseClicked

    private void btnAllTenantMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAllTenantMouseEntered
        Color color = new Color(218, 147, 163);
        panelSearchRecord.setBackground(color);
    }//GEN-LAST:event_btnAllTenantMouseEntered

    private void btnAllTenantMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAllTenantMouseExited
        Color color = new Color(218, 163, 175);
        panelSearchRecord.setBackground(color);
    }//GEN-LAST:event_btnAllTenantMouseExited

    private void btnHomeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnHomeMouseClicked
        Home h = new Home();
        h.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnHomeMouseClicked

    private void btnHomeMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnHomeMouseEntered
        Color color = new Color(218, 147, 163);
        panelHome.setBackground(color);
    }//GEN-LAST:event_btnHomeMouseEntered

    private void btnHomeMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnHomeMouseExited
        Color color = new Color(218, 163, 175);
        panelHome.setBackground(color);
    }//GEN-LAST:event_btnHomeMouseExited

    private void btnEditTenantMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditTenantMouseClicked
        Tenant tenant = new Tenant();

        tenant.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnEditTenantMouseClicked

    private void btnEditTenantMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditTenantMouseEntered
        Color color = new Color(218, 147, 163);
        panelEditc.setBackground(color);
    }//GEN-LAST:event_btnEditTenantMouseEntered

    private void btnEditTenantMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditTenantMouseExited
        Color color = new Color(218, 163, 175);
        panelEditc.setBackground(color);
    }//GEN-LAST:event_btnEditTenantMouseExited

    private void btnMonthlyRecordsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRecordsMouseClicked
        Home home = new Home();
        home.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnMonthlyRecordsMouseClicked

    private void btnMonthlyRecordsMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRecordsMouseEntered
        Color color = new Color(218, 147, 163);
        panelCourseL.setBackground(color);
    }//GEN-LAST:event_btnMonthlyRecordsMouseEntered

    private void btnMonthlyRecordsMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRecordsMouseExited
        Color color = new Color(218, 163, 175);
        panelCourseL.setBackground(color);
    }//GEN-LAST:event_btnMonthlyRecordsMouseExited

    private void btnMonthlyRentMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRentMouseClicked
        MonthlyDetails details = new MonthlyDetails();
        details.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnMonthlyRentMouseClicked

    private void btnMonthlyRentMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRentMouseEntered
        Color color = new Color(218, 147, 163);
        panelBack.setBackground(color);
    }//GEN-LAST:event_btnMonthlyRentMouseEntered

    private void btnMonthlyRentMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMonthlyRentMouseExited
        Color color = new Color(218, 163, 175);
        panelBack.setBackground(color);
    }//GEN-LAST:event_btnMonthlyRentMouseExited

    private void txtTotalAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalAmountActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MonthlyDetails.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MonthlyDetails.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MonthlyDetails.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MonthlyDetails.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MonthlyDetails().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel btnAllTenant;
    private javax.swing.JLabel btnEditTenant;
    private javax.swing.JLabel btnHome;
    private javax.swing.JLabel btnMonthlyRecords;
    private javax.swing.JLabel btnMonthlyRent;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSubmit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private com.toedter.calendar.JYearChooser jYearChooser1;
    private javax.swing.JPanel panelBack;
    private javax.swing.JPanel panelCourseL;
    private javax.swing.JPanel panelEditc;
    private javax.swing.JPanel panelHome;
    private javax.swing.JPanel panelSearchRecord;
    private javax.swing.JPanel panelSideBar;
    private javax.swing.JTable tbl_data;
    private javax.swing.JTextField txtTotalAmount;
    private com.toedter.calendar.JMonthChooser txtmonth;
    // End of variables declaration//GEN-END:variables
}
