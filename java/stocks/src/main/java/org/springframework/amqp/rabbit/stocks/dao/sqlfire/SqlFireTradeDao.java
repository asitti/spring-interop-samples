package org.springframework.amqp.rabbit.stocks.dao.sqlfire;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.stocks.dao.TradeDao;
import org.springframework.amqp.rabbit.stocks.domain.Trade;
import org.springframework.amqp.rabbit.stocks.generated.domain.QTrade;
import org.springframework.amqp.rabbit.stocks.service.DefaultTradingService;
import org.springframework.data.jdbc.query.QueryDslJdbcTemplate;
import org.springframework.data.jdbc.query.SqlInsertCallback;
import org.springframework.transaction.annotation.Transactional;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.types.Expression;
import com.mysema.query.types.MappingProjection;

@Transactional()
public class SqlFireTradeDao implements TradeDao {

	private static Log logger = LogFactory.getLog(SqlFireTradeDao.class);
	
	private final QTrade qTrade = QTrade.trade;

	private QueryDslJdbcTemplate template;

	public void setDataSource(DataSource dataSource) {
		this.template = new QueryDslJdbcTemplate(dataSource);
	}

	public void save(final Trade trade) {
		template.insert(qTrade, new SqlInsertCallback() {

			public long doInSqlInsertClause(SQLInsertClause insertClause) {
				
				return insertClause.columns(qTrade.id, qTrade.symbol, qTrade.buyrequest,  qTrade.ordertype, qTrade.quantity, qTrade.executionprice, qTrade.error, qTrade.errormessage, qTrade.confirmationnumber)
					.values(trade.getId(), trade.getSymbol(), trade.isBuyRequest() ? "T" : "F", trade.getOrdertype(), trade.getQuantity(), trade.getExecutionPrice(), trade.isError() ? "T" : "F", 
							trade.getErrorMessage(), trade.getConfirmationNumber()).execute();
				
			}
		});
		logger.info("Saved Trade " + trade);
	}

	public Trade findById(int id) {
		SQLQuery sqlQuery = template.newSqlQuery()
				.from(qTrade)
				.where(qTrade.id.eq(id));		
			return template.queryForObject(sqlQuery,  new MappingTradeProjection(qTrade.all()));
	}
	
	public Trade findByConfirmationNumber(String confirmationNumber) {
		SQLQuery sqlQuery = template.newSqlQuery()
				.from(qTrade)
				.where(qTrade.confirmationnumber.eq(confirmationNumber));		
			return template.queryForObject(sqlQuery,  new MappingTradeProjection(qTrade.all()));
	}
	
	private class MappingTradeProjection extends MappingProjection<Trade> {
		
		public MappingTradeProjection(Expression<?>... args) {
			super(Trade.class, args);
		}

		@Override
		protected Trade map(Tuple tuple) {
			Trade trade = new Trade();
			
			String errorFlag = tuple.get(qTrade.error);
			if (errorFlag.equalsIgnoreCase("T")) { 
				trade.setError(true);
				trade.setErrorMessage(tuple.get(qTrade.errormessage));
			}
			
			String buyRequestAsString = tuple.get(qTrade.buyrequest);
			if (buyRequestAsString.equalsIgnoreCase("T")) {
				trade.setBuyRequest(true);
			}
			
			
			trade.setExecutionPrice(tuple.get(qTrade.executionprice).doubleValue());
			trade.setId(tuple.get(qTrade.id));
			trade.setOrdertype(tuple.get(qTrade.ordertype));
			trade.setQuantity(tuple.get(qTrade.quantity));
			trade.setSymbol(tuple.get(qTrade.symbol));
			trade.setConfirmationNumber(tuple.get(qTrade.confirmationnumber));
			
			return trade;
		}
	}
	

}
